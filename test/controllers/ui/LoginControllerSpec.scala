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
import helpers.orchestrators.MockLoginOrchestrator
import models.{ServerCookies, User}
import orchestrators.LoginOrchestrator
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class LoginControllerSpec
  extends PlaySpec
    with Assertions
    with MockLoginOrchestrator
    with Obfuscators {

  override val locale: String = "models.ServerCookies"

  val testController: LoginController = new LoginController {
    override protected val loginOrchestrator: LoginOrchestrator = mockLoginOrchestrator
    override implicit val ec: ExecutionContext = stubControllerComponents().executionContext
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  val testIndividualUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUserName",
    email     = "test@email.com",
    emailVerified = true,
    profile = None,
    address = None,
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  "show" should {
    "return an Ok" when {
      "the page is rendered" in {
        assertFutureResult(testController.loginShow()(addCSRFToken(FakeRequest()))) {
          status(_) mustBe OK
        }
      }
    }

    "return a Redirect" when {
      "the user is already authenticated" in {
        val req = addCSRFToken(FakeRequest().withCookies(ServerCookies.createAuthCookie("test", enc = false)))

        assertFutureResult(testController.loginShow()(req)) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(uiRoutes.AccountController.show().url)
        }
      }
    }
  }

  "submit" should {
    "return an Ok" when {
      "the user has been logged in" in {
        mockAuthenticateUser(userId = Some(testIndividualUser.id))

        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test",
          "password" -> "testPass"
        ))

        assertFutureResult(testController.loginSubmit()(req)) { res =>
          status(res) mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(uiRoutes.LoginController.mfaShow().url)
        }
      }
    }

    "return a BadRequest" when {
      "the form was invalid" in {
        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test"
        ))

        assertFutureResult(testController.loginSubmit()(req)) {
          status(_) mustBe BAD_REQUEST
        }
      }

      "the user could not be authenticated" in {
        mockAuthenticateUser(userId = None)

        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test",
          "password" -> "testPass"
        ))

        assertFutureResult(testController.loginSubmit()(req)) {
          status(_) mustBe BAD_REQUEST
        }
      }
    }
  }
}
