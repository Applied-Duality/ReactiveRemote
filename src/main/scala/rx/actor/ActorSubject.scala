/**
 * Copyright 2013 Typesafe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.actor

import rx._
import rx.pure.PurePlan
import rx.impure.ImpurePlan
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.actor.Actor
import rx.pure.PureResult
import rx.impure.Sink
import scala.collection.mutable

trait Trafo[In, Out] extends (In ⇒ AsyncObservable[Out]) {
  def end(): AsyncObservable[Out]
}

class PureTrafo[In, Out](private var plan: PurePlan[In, Out]) extends Trafo[In, Out] {
  def apply(elem: In) = plan(elem) match {
    case PureResult(out, next) ⇒
      plan = next
      AsyncObservable.just(out)
    case other ⇒
      plan = other
      AsyncObservable.empty
  }
  def end() = AsyncObservable.from(plan.end())
}
object PureTrafo {
  def apply[In, Out](plan: PurePlan[In, Out]) = new PureTrafo(plan)
}

case class ImpureTrafo[In, Out](plan: ImpurePlan[In, Out]) extends Trafo[In, Out] {
  private var _result: List[Out] = Nil
  plan.setSink(new Sink[Out] {
    def apply(elem: Out): Unit = _result ::= elem
    def end(): Unit = ()
  })
  private def result = {
    val ret = _result.reverse
    _result = Nil
    ret
  }
  def apply(elem: In) = {
    plan(elem)
    result match {
      case Nil      ⇒ AsyncObservable.empty
      case x :: Nil ⇒ AsyncObservable.just(x)
      case other    ⇒ sys.error("must not happen")
    }
  }
  def end() = {
    plan.end()
    AsyncObservable.from(result.reverse)
  }
}

case class FlatMap[In, Out](f: In ⇒ AsyncObservable[Out]) extends Trafo[In, Out] {
  def apply(elem: In) = f(elem)
  def end() = AsyncObservable.empty
}

private[actor] class ActorSubject[In, Out](settings: Settings, trafo: Trafo[In, Out], name: String) extends AsyncObserver[In] with AsyncObservable[Out] {

  override def onInit(subscription: AsyncSubscription) = actor ! Init(subscription)
  override def onNext(elem: In): Unit = actor ! Next(elem)
  override def onError(cause: Throwable): Unit = actor ! Error(cause)
  override def onComplete(): Unit = actor ! Complete

  override def subscribe(observer: AsyncObserver[Out]): AsyncSubscription = {
    val s = Subscription(observer)
    actor ! s
    s
  }

  private case class Init(subscription: AsyncSubscription)
  private case class Next(elem: In)
  private case class Error(cause: Throwable)
  private case object Complete

  private case class Request(observer: AsyncObserver[Out], elems: Int)
  private case class Unsubscribe(observer: AsyncObserver[Out])

  private case class Subscription(observer: AsyncObserver[Out]) extends AsyncSubscription {
    def requestNext(elems: Int): Unit = actor ! Request(observer, elems)
    def unsubscribe(): Unit = actor ! Unsubscribe(observer)
  }

  private case class SubNext(elem: Out)
  private case class SubError(cause: Throwable)
  private case object SubComplete

  private val actor = settings.ctx.actorOf(Props(classOf[Runner], this), name)

  private val Stop = AsyncObservable.error(new Exception("The Stop Marker"))

  private class Runner extends Actor {
    // subscriber -> outstandingRequestedElems
    val subscribers = mutable.Map[AsyncObserver[Out], Int]()
    var streams = Vector.empty[AsyncObservable[Out]]
    var currentSubscription: AsyncSubscription = _
    val fanOutBox = settings.fanOutFactory()

    def receive = {
      case Init(upstream) ⇒
        context.become(initialized(upstream))
    }

    def initialized(upstream: AsyncSubscription): Receive = {
      case Next(elem) ⇒
        enqueue(trafo(elem))
      case Error(cause) ⇒
        enqueue(AsyncObservable.error(cause))
      case Complete ⇒
        enqueue(Stop)
      case Request(observer, elems) ⇒
      // TODO
      case Unsubscribe(obs)         ⇒
      // TODO
      case SubNext(elem: AnyRef)    ⇒ fanOutBox.onNext(elem)
      // TODO hook things up the current stream to request more
      case SubError(cause)          ⇒ fanOutBox.onError(cause)
      case SubComplete              ⇒ fanOutBox.onComplete()
      // TODO switch streams
      case subscription @ Subscription(obs) ⇒
        obs.onInit(subscription)
        subscribers += obs -> 0
    }

    def enqueue(obs: AsyncObservable[Out]): Unit = {
      if (streams.isEmpty && subscribers.nonEmpty) {
        hookup(obs)
      }
      streams :+= obs
    }

    def hookup(obs: AsyncObservable[Out]): Unit = {
      currentSubscription = obs.subscribe(new AsyncObserver[Out] {
        override def onInit(sub: AsyncSubscription): Unit = ()
        override def onNext(elem: Out): Unit = self ! SubNext(elem)
        override def onError(cause: Throwable): Unit = self ! SubError(cause)
        override def onComplete(): Unit = self ! SubComplete
      })
    }

  }

}