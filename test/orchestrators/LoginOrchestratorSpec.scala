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
import helpers.services.{MockLoginService, MockTOTPService}
import models.{Login, LoginAttempt, User}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.{FailedGeneration, LoginService, Secret, TOTPService}

import scala.concurrent.ExecutionContext.Implicits.global

class LoginOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockLoginService
    with MockTOTPService {

  val testOrchestrator: LoginOrchestrator = new LoginOrchestrator {
    override protected val loginService: LoginService = mockLoginService
    override protected val totpService: TOTPService = mockTOTPService
  }

  val now: DateTime = new DateTime()

  val testUser: User = User(
    id        = "testId",
    userName  = "testUsername",
    email     = "test@email.com",
    emailVerified = true,
    profile = None,
    address = None,
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    mfaSecret = None,
    mfaEnabled = false,
    authorisedClients = List(),
    createdAt = now
  )

  "authenticateUser" should {
    "return a users id" when {
      "the users salt has been found and the users password has been validated" in {
        mockGetUserSalt(salt = Some("testSalt"))
        mockValidateUser(user = Some(testUser))
        mockSaveLoginAttempt(success = true)

        awaitAndAssert(testOrchestrator.authenticateUser(Login("testUserName", "testPassword"))) { res =>
          assert(res.isDefined)
          assert(res.get.matches("^att-\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b$"))
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

  "mfaChallengePresenter" should {
    "TOTPMFAChallenge" when {
      "the user should be presented with an MFA challenge" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = Some(testUser.id))
        mockGetMFAStatus(resp = true)

        awaitAndAssert(testOrchestrator.mfaChallengePresenter(loginAttempt.id)) {
          _ mustBe TOTPMFAChallenge
        }
      }
    }

    "NoMFAChallengeNeeded" when {
      "the user does not have MFA enabled" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = Some(testUser.id))
        mockGetMFAStatus(resp = false)

        awaitAndAssert(testOrchestrator.mfaChallengePresenter(loginAttempt.id)) {
          _ mustBe NoMFAChallengeNeeded(testUser.id)
        }
      }
    }

    "InvalidLogonAttempt" should {
      "the logon attempt could be found" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = None)

        awaitAndAssert(testOrchestrator.mfaChallengePresenter(loginAttempt.id)) {
          _ mustBe InvalidLogonAttempt
        }
      }
    }
  }

  "verifyMFAChallenge" should {
    "return MFAValidated" when {
      "the provided verification code was validated against the users secret" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = Some(testUser.id))
        mockGetCurrentSecret(resp = Secret("testSecret"))
        mockValidateCodes(resp = true)

        awaitAndAssert(testOrchestrator.verifyMFAChallenge(loginAttempt.id, "testCode")) {
          _ mustBe MFAValidated(testUser.id)
        }
      }
    }

    "return MFAInvalid" when {
      "the provided verification code could not be validated against the users secret" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = Some(testUser.id))
        mockGetCurrentSecret(resp = Secret("testSecret"))
        mockValidateCodes(resp = false)

        awaitAndAssert(testOrchestrator.verifyMFAChallenge(loginAttempt.id, "testCode")) {
          _ mustBe MFAInvalid
        }
      }
    }

    "return an MFAError" when {
      "the logon attempt could not be found" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = None)


        awaitAndAssert(testOrchestrator.verifyMFAChallenge(loginAttempt.id, "testCode")) {
          _ mustBe MFAError
        }
      }

      "the users MFA secret could not be found" in {
        val loginAttempt = LoginAttempt(
          userId = testUser.id,
          success = true
        )

        mockLookupLoginAttempt(userId = Some(testUser.id))
        mockGetCurrentSecret(resp = FailedGeneration)

        awaitAndAssert(testOrchestrator.verifyMFAChallenge(loginAttempt.id, "testCode")) {
          _ mustBe MFAError
        }
      }
    }
  }
}
