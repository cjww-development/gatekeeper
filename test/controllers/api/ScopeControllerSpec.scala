/*
 * Copyright 2020 CJWW Development
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

package controllers.api

import com.cjwwdev.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.services.MockScopeService
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ScopeService

class ScopeControllerSpec
  extends PlaySpec
    with Assertions
    with MockScopeService
    with Obfuscators {

  override val locale: String = "models.ServerCookies"

  val testController: ScopeController = new ScopeController {
    override val scopesService: ScopeService = mockScopeService
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  "getValidScopes" should {
    "return an Ok" when (
      "there are valid scopes" in {
        mockGetScopes(populated = true)

        assertFutureResult(testController.getValidScopes()(FakeRequest())) { res =>
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.parse(
            """
              |{
              |   "reads": ["testRead"],
              |   "writes": ["testWrite"]
              |}
            """.stripMargin
          )
        }
      }
    )

    "return a No Content" when {
      "there are no valid scopes" in {
        mockGetScopes(populated = false)

        assertFutureResult(testController.getValidScopes()(FakeRequest())) { res =>
          status(res) mustBe NO_CONTENT
        }
      }
    }
  }
}
