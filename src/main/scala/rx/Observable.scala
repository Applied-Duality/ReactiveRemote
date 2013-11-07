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

/*
 * These are just copies of the main API to sketch things out; it should be
 * trivial to make them type aliases and pull in the real thing.
 */

trait Observable[+T] {
  def subscribe(observer: Observer[T]): Subscription
}

trait Observer[-T] {
  def onNext(element: T): Unit
  def onComplete(): Unit
  def onError(cause: Throwable): Unit
}

trait Subscription {
  def unsubscribe(): Unit
}