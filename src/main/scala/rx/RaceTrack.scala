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

package rx

import scala.collection.mutable
import scala.annotation.tailrec

object RaceTrack {

}

/**
 * This is the implementation of the race track multiple-consumer back-pressure 
 * model for the case of matching the pace to the slowest consumer.
 */
class RaceTrack(bufferSize: Int) extends FanOutBox {
  import FanOutBox._

  case class Error(cause: Throwable)
  case object Complete

  private var _state: State = Ready
  // observer -> elements asked for
  private val outputs = mutable.Map[AsyncObserver[_], Int]()
  private var buffer = Vector.empty[AnyRef]
  /*
   * The semantics are:
   * 
   * - baseline is the last element which has been completely disseminated,
   *   i.e. one before where the buffer starts
   * - the entries of the `outputs` map refer to where an observer wants to be
   */
  private var baseline = 0

  private def checkState(): Unit = _state match {
    case Finished  ⇒
    case Finishing ⇒ if (buffer.size == 0) _state = Finished
    case _         ⇒ if (buffer.size == bufferSize) _state = Blocked else _state = Ready
  }

  private def enqueue(elem: AnyRef): Unit = {
    buffer :+= elem
    checkState()
  }

  @tailrec private def send(start: Int, count: Int, obs: AsyncObserver[_]): Unit = {
    val pos = start - baseline
    if (pos < buffer.size && count > 0) buffer(pos) match {
      case Complete   ⇒ obs.onComplete()
      case Error(thr) ⇒ obs.onError(thr)
      case other      ⇒ obs.asInstanceOf[AsyncObserver[AnyRef]].onNext(other)
    }
    send(start + 1, count - 1, obs)
  }

  private def prune(): Unit =
    if (outputs.nonEmpty) {
      val min = outputs.valuesIterator.map(_ - baseline).reduce(Math.min)
      if (min > 0) {
        baseline += min
        buffer = buffer.drop(min)
        checkState()
      }
    }

  override def addReceiver(obs: AsyncObserver[_]): Unit = {
    outputs(obs) = baseline
    if (bufferSize == 0) _state = Blocked
  }
  override def removeReceiver(obs: AsyncObserver[_]): Unit = {
    outputs -= obs
    if (_state == Blocked) checkState()
  }

  override def state = _state

  override def onNext(elem: AnyRef) = state match {
    case s @ (Blocked | Finishing | Finished) ⇒
      throw new IllegalStateException("cannot push element while " + s)
    case Ready ⇒
      enqueue(elem)
      outputs.iterator foreach {
        case (obs, req) ⇒ if (req - baseline >= buffer.size) send(baseline + buffer.size - 1, 1, obs)
      }
  }

  override def onError(cause: Throwable) = {
    enqueue(Error(cause))
    _state = Finishing
    // TODO make it work ;-)
  }

  override def onComplete() = {
    enqueue(Complete)
    _state = Finishing
    // TODO make it work ;-)
  }

  override def requestMore(obs: AsyncObserver[_], elems: Int) = {
    val start = outputs(obs)
    outputs(obs) = start + elems
    send(start + 1, elems, obs)
    prune()
  }

}
