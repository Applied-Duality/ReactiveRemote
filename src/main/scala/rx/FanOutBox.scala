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

object FanOutBox {
  sealed trait State
  case object Ready extends State
  case object Blocked extends State
  case object Finishing extends State
  case object Finished extends State
}

trait FanOutBox {
  import FanOutBox._
  
  def state: State
  def onNext(elem: AnyRef): Unit
  def onError(cause: Throwable): Unit
  def onComplete(): Unit
  def addReceiver(obs: AsyncObserver[_]): Unit
  def removeReceiver(obs: AsyncObserver[_]): Unit
  def requestMore(obs: AsyncObserver[_], elems: Int): Unit
}