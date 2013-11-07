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

import rx.PlanSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class RxPlanSpec extends WordSpec with ShouldMatchers with PlanSpec {
  type R[I, O] = RxPlan[I, O]
  
  "An RxPlan" when {
    testPlan(RxPlan.in[Int], p => new RxPlanRunner(p))
  }
}