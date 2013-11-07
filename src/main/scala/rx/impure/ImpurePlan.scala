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

package rx.impure

import rx.Plan

/**
 * This plan produces elements e.g. by calling onNext() on the next stage of the stream processing.
 */
trait ImpurePlan[In, Out] extends Plan[In, Out] with Sink[In] with HasSink[Out]

trait HasSink[T] {
  protected[rx] def sink: Sink[T]
}

trait Sink[T] {
  def apply(elem: T): Unit
  def end(): Unit
}

abstract class RxPlan[In, Out] extends ImpurePlan[In, Out] { outer ⇒
  
  type Repr[I, O] = RxPlan[I, O]
  
  private[this] var next: Sink[Out] = null
  override protected[rx] def sink = next
  private[rx] def setSink(n: Sink[Out]) = next match {
    case null ⇒ next = n
    case _    ⇒ throw new IllegalStateException("cannot chain twice off the same RxPlan")
  }
  def outerst: Sink[In]
  override def apply(elem: In): Unit = outerst(elem)
  override def end(): Unit = outerst.end()

  override def filter(p: Out ⇒ Boolean): RxPlan[In, Out] = new Step[Out] {
    def transform = Filter(p, this)
  }
  override def fold[Z](z: Z)(f: (Z, Out) ⇒ Z): RxPlan[In, Z] = new Step[Z] {
    def transform = Fold(z, f, this)
  }
  override def map[NewOut](f: Out => NewOut): RxPlan[In, NewOut] = new Step[NewOut] {
    def transform = Map(f, this)
  }

  abstract class Step[T] extends RxPlan[In, T] {
    def transform: Sink[Out]
    outer.setSink(transform)
    val outerst = outer.outerst
  }
}

object RxPlan {
  def in[In](): RxPlan[In, In] = new RxPlan[In, In]() {
    val outerst = this
    override def apply(elem: In): Unit = sink(elem)
    override def end(): Unit = sink.end()
  }
}

abstract class Transform[In, Out](protected val s: HasSink[Out]) extends Sink[In] {
  def end(): Unit = s.sink.end()
}

final case class Filter[T](p: T ⇒ Boolean, _s: HasSink[T]) extends Transform[T, T](_s) {
  def apply(elem: T): Unit = if (p(elem)) s.sink(elem)
}

final case class Fold[T, Z](var z: Z, f: (Z, T) ⇒ Z, _s: HasSink[Z]) extends Transform[T, Z](_s) {
  def apply(elem: T): Unit = z = f(z, elem)
  override def end(): Unit = {
    s.sink(z)
    super.end()
  }
}

final case class Map[T, U](f: T => U, _s: HasSink[U]) extends Transform[T, U](_s) {
  def apply(elem: T): Unit = s.sink(f(elem))
}
