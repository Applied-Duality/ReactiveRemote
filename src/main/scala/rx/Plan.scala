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
 **/

package rx

/**
 * A Plan operates on an incoming stream of elements, producing a stream of output elements.
 * 
 * This interface does not describe the evaluation strategy for a Plan, which is left to subtypes.
 * 
 * A concrete Plan implementation must also provide a means to signal end-of-stream.
 */
trait Plan[In, Out] {
  type Repr[I, O] <: Plan[I, O]
  def map[NewOut](f: Out => NewOut): Repr[In, NewOut] = ???
  def mapWithIndex[NewOut](f: (Out, Long) => NewOut): Repr[In, NewOut] = ???
  def collect[NewOut](f: PartialFunction[Out, NewOut]): Repr[In, NewOut] = ???
  def filter(p: Out => Boolean): Repr[In, Out] = ???
  def fold[Z](zero: Z)(f: (Z, Out) => Z): Repr[In, Z] = ???
  def scan[Z](zero: Z)(f: (Z, Out) => Z): Repr[In, Z] = ???
  def reduce(f: (Out, Out) => Out): Repr[In, Out] = ???
  def reduceScan(f: (Out, Out) => Out): Repr[In, Out] = ???
  def forall(p: Out => Boolean): Repr[In, Boolean] = ???
  def exists(p: Out => Boolean): Repr[In, Boolean] = ???
  def drop(n: Long): Repr[In, Out] = ???
  def dropWhile(p: Out => Boolean): Repr[In, Out] = ???
  def take(n: Long): Repr[In, Out] = ???
  def takeWhile(p: Out => Boolean): Repr[In, Out] = ???
  def contains(elem: Out): Repr[In, Boolean] = ???
  def count(): Repr[In, Long] = ???
  def distinct(): Repr[In, Out] = ???
  def first(): Repr[In, Out] = ???
  def last(): Repr[In, Out] = ???
  def fallback(elem: Out): Repr[In, Out] = ??? // produces `elem` if input stream was empty
}

