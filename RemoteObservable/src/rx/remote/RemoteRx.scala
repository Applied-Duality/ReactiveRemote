/**
 * Copyright 2013 Applied Duality, Inc.
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

package rx.remote

import scala.util.Try

/**
 * Futures as singleton observables
 */
trait Task[T] extends rx.Observable[T] {
  def continueWith[S](f: Task[T] => S): Task[S]
  def subscribe(f: Try[T] => Unit): Unit
  def result(): T
}

/**
 * Remote observable
 */
trait Observable[+T] {
  def subscribe(observer: Observer[T]) : Task[Subscription]
}

/**
 * Remote observer
 */
trait Observer[-T] {
  def onNext(value: T): Task[Unit]
  def onError(error: Throwable): Task[Unit]
  def onCompleted(): Task[Unit]
}

/**
 * Remote subscription
 */
trait Subscription {
  def unsubscribe(): Task[Unit]
  def isUnsubscribed(): Task[Boolean]
}

/**
 * Remote scheduler
 */
trait Scheduler {
  def schedule(work: =>Unit): Task[rx.Subscription]
  def schedule(work: rx.Scheduler => rx.Subscription): Task[rx.Subscription]
}
