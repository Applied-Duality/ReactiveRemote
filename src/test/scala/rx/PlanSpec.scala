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

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

trait PlanSpec { this: WordSpec with ShouldMatchers =>
  
  type R[I, O] <: Plan[I, O]
  
  // couldn’t figure out how to make a recursive type declaration work, hence covered explicitly up to depth two
  def testPlan(plan: => R[Int, Int] { type Repr[I, O] <: R[I, O] { type Repr[I, O] <: R[I, O] }}, mk: R[Int, String] => PlanRunner[Int, String]): Unit = {
    "mapped" should {
      "contain the right elements" in {
        val p = mk(plan.map(_.toString))
        p(1) should be(Some("1"))
        p(2) should be(Some("2"))
        p.end() should be(Seq())
      }
    }
    "folded" should {
      "contain the right elements" in {
        val p = mk(plan.fold(0)(_ + _).map(_.toString))
        p(1) should be(None)
        p(2) should be(None)
        p.end() should be(Seq("3"))
      }
    }
    "filtered" should {
      "contain the right elements" in {
        val p = mk(plan.filter(_ > 2).map(_.toString))
        p(1) should be(None)
        p(3) should be(Some("3"))
        p.end() should be(Seq())
      }
    }
  }
  
}