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

package rx.pure

import scala.collection.immutable
import rx.Plan

/**
 * The idea here is that PurePlan instances can be freely shared, stored, retried, etc.
 * This has some overhead for the allocation of extra objects when generating output.
 */
sealed abstract class PurePlan[In, Out] extends Plan[In, Out] {
  type Repr[I, O] = PurePlan[I, O]

  def apply(elem: In): PurePlan[In, Out]
  def end(): immutable.Seq[Out]

  override def filter(p: Out ⇒ Boolean): Repr[In, Out] = Filter(p, this)
  override def fold[Z](z: Z)(f: (Z, Out) ⇒ Z): Repr[In, Z] = Fold(z, f, this)
  override def map[NewOut](f: Out ⇒ NewOut): Repr[In, NewOut] = Map(f, this)
}

abstract class ComposedPurePlan[In, T, Out](prev: PurePlan[In, T]) extends PurePlan[In, Out] {
  /**
   * Transform one element and return a new plan which also carries the updated nested plan.
   */
  def transform(elem: T, prev: PurePlan[In, T]): PurePlan[In, Out]
  /**
   * This performs a behavior step for the nested plan while keeping the outer plan the same.
   */
  def step[X](prev: PurePlan[X, T]): ComposedPurePlan[X, T, Out]
  /**
   * This method returns the accumulated state of this plan (if any; nested plan not included) at the end of the stream.
   */
  def finalElem(): Option[Out] = None

  final override def apply(elem: In): PurePlan[In, Out] = {
    prev(elem) match {
      case PureResult(res, next) ⇒ transform(res, next)
      case other                 ⇒ step(other)
    }
  }
  final override def end(): immutable.Seq[Out] = {
    val elems = prev.end()
    val me: PurePlan[T, Out] = step(Identity())
    val (acc, stage) = elems.foldLeft((Vector.empty[Out], me)) { (pair, elem) ⇒
      val (acc, stage) = pair
      stage(elem) match {
        case PureResult(res, next) ⇒ (acc :+ res, next)
        case other                 ⇒ (acc, other)
      }
    }
    stage match {
      case c: ComposedPurePlan[T, T, Out] ⇒ acc ++ c.finalElem()
      case other                          ⇒ acc ++ other.end()
    }
  }
}

object PurePlan {
  def in[In]: PurePlan[In, In] = Identity() // this can later be optimized
}

/**
 * This is the vehicle for producing elements from a PurePlan.
 */
case class PureResult[In, Out](elem: Out, nextPlan: PurePlan[In, Out]) extends PurePlan[In, Out] {
  override def apply(elem: In) = throw new UnsupportedOperationException("must unwrap PureResult for processing")
  override def end() = throw new UnsupportedOperationException("must unwrap PureResult for processing")
}

final case class Identity[T]() extends PurePlan[T, T] {
  override def apply(elem: T): PurePlan[T, T] = PureResult(elem, this)
  override def end(): immutable.Seq[T] = Vector.empty
}

final case class End[In, Out]() extends PurePlan[In, Out] {
  override def apply(elem: In): PurePlan[In, Out] = this
  override def end(): immutable.Seq[Out] = Vector.empty
}

final case class Filter[In, Out](p: Out ⇒ Boolean, prev: PurePlan[In, Out]) extends ComposedPurePlan[In, Out, Out](prev) {
  override def transform(in: Out, next: PurePlan[In, Out]) = if (p(in)) PureResult(in, Filter(p, next)) else this
  override def step[X](next: PurePlan[X, Out]) = Filter(p, next)
}

final case class Fold[In, Out, Z](z: Z, f: (Z, Out) ⇒ Z, prev: PurePlan[In, Out]) extends ComposedPurePlan[In, Out, Z](prev) {
  override def transform(in: Out, next: PurePlan[In, Out]) = Fold(f(z, in), f, next)
  override def step[X](next: PurePlan[X, Out]) = Fold(z, f, next)
  override def finalElem() = Some(z)
}

final case class Map[In, Out, NewOut](f: Out ⇒ NewOut, prev: PurePlan[In, Out]) extends ComposedPurePlan[In, Out, NewOut](prev) {
  override def transform(in: Out, next: PurePlan[In, Out]) = PureResult(f(in), Map(f, next))
  override def step[X](next: PurePlan[X, Out]) = Map(f, next)
}
