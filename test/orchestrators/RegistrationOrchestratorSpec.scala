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

package orchestrators

import helpers.services.MockRegistrationService
import models.User
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import services.RegistrationService

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationOrchestratorSpec extends PlaySpec with FutureAwaits with DefaultAwaitTimeout with MockRegistrationService {

  val testOrchestrator: RegistrationOrchestrator = new RegistrationOrchestrator {
    override val registrationService: RegistrationService = mockRegistrationService
  }

  val testUser: User = User(
    id       = "testId",
    userName = "testUsername",
    email    = "test@email.com",
    accType  = "ORGANISATION",
    password = "testPassword"
  )

  "registerUser" should {
    "return a Registered response" when {
      "the user is successfully registered" in {
        mockValidateEmail(inUse = false)
        mockValidateUserName(inUse = false)
        mockCreateNewUser(success = true)

        val res = await(testOrchestrator.registerUser(testUser))
        res mustBe Registered
      }
    }

    "return an EmailInUse response" when {
      "the new users email is already in use" in {
        mockValidateEmail(inUse = true)
        mockValidateUserName(inUse = false)

        val res = await(testOrchestrator.registerUser(testUser))
        res mustBe EmailInUse
      }
    }

    "return a UserNameInUse response" when {
      "the new users user name is already in use" in {
        mockValidateEmail(inUse = false)
        mockValidateUserName(inUse = true)

        val res = await(testOrchestrator.registerUser(testUser))
        res mustBe UserNameInUse
      }
    }

    "return a BothInUse response" when {
      "the new users email and user name are already in use" in {
        mockValidateEmail(inUse = true)
        mockValidateUserName(inUse = true)

        val res = await(testOrchestrator.registerUser(testUser))
        res mustBe BothInUse
      }
    }

    "return a RegistrationError response" when {
      "there was a problem registering the new user" in {
        mockValidateEmail(inUse = false)
        mockValidateUserName(inUse = false)
        mockCreateNewUser(success = false)

        val res = await(testOrchestrator.registerUser(testUser))
        res mustBe RegistrationError
      }
    }
  }
}
