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

import rx.PlanSpec
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

class PurePlanSpec extends WordSpec with ShouldMatchers with PlanSpec {
  type R[I, O] = PurePlan[I, O]
  
  "A PurePlan" when {
    testPlan(PurePlan.in[Int], p => new PurePlanRunner(p))
  }
}