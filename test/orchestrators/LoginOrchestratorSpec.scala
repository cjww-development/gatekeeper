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

import helpers.Assertions
import helpers.services.MockLoginService
import models.{Login, User}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.LoginService

import scala.concurrent.ExecutionContext.Implicits.global

class LoginOrchestratorSpec extends PlaySpec with Assertions with MockLoginService {

  val testOrchestrator: LoginOrchestrator = new LoginOrchestrator {
    override protected val loginService: LoginService = mockLoginService
  }

  val testUser: User = User(
    id        = "testId",
    userName  = "testUsername",
    email     = "test@email.com",
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    mfaSecret = None,
    mfaEnabled = false,
    authorisedClients = List(),
    createdAt = DateTime.now()
  )

  "authenticateUser" should {
    "return a User" when {
      "the users salt has been found and the users password has been validated" in {
        mockGetUserSalt(salt = Some("testSalt"))
        mockValidateUser(user = Some(testUser))

        awaitAndAssert(testOrchestrator.authenticateUser(Login("testUserName", "testPassword"))) {
          _ mustBe Some(testUser)
        }
      }
    }

    "return None" when {
      "no salt could be found for the user" in {
        mockGetUserSalt(salt = None)

        awaitAndAssert(testOrchestrator.authenticateUser(Login("testUserName", "testPassword"))) {
          _ mustBe None
        }
      }

      "the users password could not be validated" in {
        mockGetUserSalt(salt = Some("testSalt"))
        mockValidateUser(user = None)

        awaitAndAssert(testOrchestrator.authenticateUser(Login("testUserName", "testPassword"))) {
          _ mustBe None
        }
      }
    }
  }
}
