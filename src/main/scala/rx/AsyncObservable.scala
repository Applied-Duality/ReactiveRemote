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

trait AsyncObservable[+T] {
  def subscribe(observer: AsyncObserver[T]): AsyncSubscription
}

trait AsyncObserver[-T] {
  def onInit(subscription: AsyncSubscription): Unit
  def onNext(element: T): Unit
  def onComplete(): Unit
  def onError(cause: Throwable): Unit
}

trait AsyncSubscription extends Subscription {
  def requestNext(elements: Int): Unit
}