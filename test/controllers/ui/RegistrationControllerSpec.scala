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

import helpers.Assertions
import helpers.orchestrators.MockRegistrationOrchestrator
import orchestrators.{AccountIdsInUse, Registered, RegistrationError, RegistrationOrchestrator}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.CSRFTokenHelper.addCSRFToken
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class RegistrationControllerSpec
  extends PlaySpec
    with Assertions
    with MockRegistrationOrchestrator {

  val testController: RegistrationController = new RegistrationController {
    override val registrationOrchestrator: RegistrationOrchestrator = mockRegistrationOrchestrator
    override implicit val ec: ExecutionContext = stubControllerComponents().executionContext
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  "show" should {
    "return an Ok" when {
      "the page is rendered" in {
        assertFutureResult(testController.showUserReg()(addCSRFToken(FakeRequest()))) {
          status(_) mustBe OK
        }
      }
    }
  }

  "submit" should {
    "return an Ok" when {
      "the user has been registered" in {
        mockRegisterUser(result = Registered)

        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test",
          "email" -> "test@email.com",
          "accType" -> "individual",
          "password" -> "testPass",
          "confirmPassword" -> "testPass"
        ))

        assertFutureResult(testController.submitUserReg()(req)) {
          status(_) mustBe OK
        }
      }
    }

    "return a BadRequest" when {
      "the form was invalid" in {
        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test"
        ))

        assertFutureResult(testController.submitUserReg()(req)) {
          status(_) mustBe BAD_REQUEST
        }
      }

      "there was a error during registration" in {
        mockRegisterUser(result = RegistrationError)

        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test",
          "email" -> "test@email.com",
          "accType" -> "individual",
          "password" -> "testPass",
          "confirmPassword" -> "testPass"
        ))

        assertFutureResult(testController.submitUserReg()(req)) {
          status(_) mustBe BAD_REQUEST
        }
      }

      "the users email or username is already in use" in {
        mockRegisterUser(result = AccountIdsInUse)

        val req = addCSRFToken(FakeRequest().withFormUrlEncodedBody(
          "userName" -> "test",
          "email" -> "test@email.com",
          "accType" -> "individual",
          "password" -> "testPass",
          "confirmPassword" -> "testPass"
        ))

        assertFutureResult(testController.submitUserReg()(req)) {
          status(_) mustBe BAD_REQUEST
        }
      }
    }
  }
}
