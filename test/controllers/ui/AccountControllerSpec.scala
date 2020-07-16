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

package controllers.ui

import java.util.UUID

import com.cjwwdev.security.Implicits.ImplicitObfuscator
import com.cjwwdev.security.obfuscation.Obfuscators
import controllers.ui.{routes => uiRoutes}
import helpers.Assertions
import helpers.orchestrators.{MockLoginOrchestrator, MockUserOrchestrator}
import models.{ServerCookies, User}
import orchestrators.{LoginOrchestrator, UserOrchestrator}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class AccountControllerSpec
  extends PlaySpec
    with Assertions
    with MockUserOrchestrator
    with Obfuscators {

  override val locale: String = "models.ServerCookies"

  val testController: AccountController = new AccountController {
    override protected val userOrchestrator: UserOrchestrator = mockUserOrchestrator
    override implicit val ec: ExecutionContext = stubControllerComponents().executionContext

    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  val testIndividualUser: User = User(
    id = s"user-${UUID.randomUUID()}",
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword",
    salt = "testSalt",
    createdAt = DateTime.now()
  )

  "show" should {
    "return an Ok" when {
      "the user is authenticated and details have been found" in {
        val req = FakeRequest().withCookies(ServerCookies.createAuthCookie(testIndividualUser.id, enc = true))

        mockGetUserDetails(details = Map(
          "userName" -> testIndividualUser.userName,
          "email" -> testIndividualUser.email,
          "createdAt" -> "2020-01-01"
        ))

        assertFutureResult(testController.show()(req)) {
          status(_) mustBe OK
        }
      }
    }

    "return a Redirect" when {
      "the user isn't authenticated" in {
        val req = FakeRequest()

        assertFutureResult(testController.show()(req)) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(uiRoutes.LoginController.show().url)
        }
      }
    }
  }
}
