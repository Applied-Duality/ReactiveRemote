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

import rx.PlanRunner

class RxPlanRunner[In, Out](plan: RxPlan[In, Out]) extends PlanRunner[In, Out] {
  private val builder = Vector.newBuilder[Out]
  private var endCalled = false
  private val sink = new Sink[Out] {
    def apply(elem: Out): Unit = builder += elem
    def end(): Unit = endCalled = true
  }
  plan.setSink(sink)
  private def result() = {
    val res = builder.result()
    builder.clear()
    res
  }
  
  def ended: Boolean = endCalled
  def apply(elem: In): Option[Out] = {
    plan(elem)
    val res = result()
    assert(res.size <= 1, s"size was ${res.size}")
    res.headOption
  }
  def end(): Seq[Out] = {
    plan.end()
    val res = builder.result()
    assert(endCalled, "end() was not called")
    res
  }
}