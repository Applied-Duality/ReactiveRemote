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

import rx.PlanRunner

class PurePlanRunner[In, Out](_plan: PurePlan[In, Out]) extends PlanRunner[In, Out] {
  private var plan = _plan
  def apply(elem: In): Option[Out] = plan(elem) match {
    case PureResult(res, next) =>
      plan = next
      Some(res)
    case other =>
      plan = other
      None
  }
  def end(): Seq[Out] = plan.end()
  def ended: Boolean = plan.isInstanceOf[End[_, _]]
}