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

import rx.AsyncObservable
import rx.AsyncMonad
import rx.AsyncObservable
import akka.actor.ActorRefFactory
import rx.FanOutBox

case class Settings(window: Int, batchSize: Int, ctx: ActorRefFactory, fanOutFactory: () => FanOutBox)

object ActorAsync {
  def apply[T](obs: AsyncObservable[T], settings: Settings, name: String = ""): AsyncMonad[T] = new ActorAsync(obs, settings, name)
  /** Java API */
  def actorAsync[T](obs: AsyncObservable[T], settings: Settings): AsyncMonad[T] = apply(obs, settings)
  /** Java API */
  def actorAsync[T](obs: AsyncObservable[T], settings: Settings, name: String): AsyncMonad[T] = apply(obs, settings, name)
}

class ActorAsync[T](observable: AsyncObservable[T], settings: Settings, name: String) extends AsyncMonad[T] {

  def run: AsyncObservable[T] = observable
  def flatMap[U](f: T => AsyncMonad[U]): AsyncMonad[U] = new FlatMapper(f)
  
  private class FlatMapper[U](f: T => AsyncMonad[U]) extends AsyncMonad[U] {
    def run: AsyncObservable[U] = new ActorSubject(settings, FlatMap(f andThen (_.run)), name)
    def flatMap[UU](ff: U => AsyncMonad[UU]): AsyncMonad[UU] = new FlatMapper(f(_) flatMap ff)
  }
  
}
