/*
 * Copyright 2021 CJWW Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.oauth2

import helpers.Assertions
import models.Scope
import org.scalatestplus.play.PlaySpec

class ScopeServiceSpec
  extends PlaySpec
    with Assertions {

  private val testService: ScopeService = new ScopeService {
    override protected val approvedScopes: Seq[Scope] = Seq(
      Scope(name = "read:testRead", readableName = "read:testRead", desc = ""),
      Scope(name = "write:testWrite", readableName = "write:testWrite", desc = "")
    )
  }

  "getValidScopes" should {
    "return a Scopes object" in {
      assertOutput(testService.getValidScopes) {
        _ mustBe Seq(
          Scope(name = "read:testRead", readableName = "read:testRead", desc = ""),
          Scope(name = "write:testWrite", readableName = "write:testWrite", desc = "")
        )
      }
    }
  }

  "getScopeDetails" should {
    "return a sequence of scopes matching the scope names" in {
      assertOutput(testService.getScopeDetails(strScopes = Seq("read:testRead", "write:testWrite"))) {
        _ mustBe Seq(
          Scope(name = "read:testRead", readableName = "read:testRead", desc = ""),
          Scope(name = "write:testWrite", readableName = "write:testWrite", desc = "")
        )
      }
    }
  }

  "validateScopes" should {
    "return true" when {
      "the requested scopes are valid" in {
        assertOutput(testService.validateScopes(scopes = "read:testRead write:testWrite")) {
          res => assert(res)
        }
      }
    }

    "return false" when {
      "the requested scopes aren't valid" in {
        assertOutput(testService.validateScopes(scopes = "testRead testWrite")) {
          res => assert(!res)
        }
      }
    }
  }

  "validateScopes (app scopes)" should {
    "return true" when {
      "the requested scopes are valid against the apps scopes" in {
        assertOutput(testService.validateScopes(scopes = "read:testRead write:testWrite", appScopes = Seq("read:testRead", "write:testWrite"))) {
          res => assert(res)
        }
      }
    }

    "return false" when {
      "the requested scopes aren't valid against the apps scopes" in {
        assertOutput(testService.validateScopes(scopes = "read:testRead write:testWrite", appScopes = Seq("read:testRead"))) {
          res => assert(!res)
        }
      }
    }
  }
}
