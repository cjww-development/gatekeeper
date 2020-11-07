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

import java.util.UUID

import helpers.Assertions
import helpers.services.MockTOTPService
import models.User
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services._

import scala.concurrent.ExecutionContext.Implicits.global

class MFAOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockTOTPService {

  val testOrchestrator: MFAOrchestrator = new MFAOrchestrator {
    override val totpService: TOTPService = mockTOTPService
  }

  val testUser: User = User(
    id        = s"org-user-${UUID.randomUUID().toString}",
    userName  = "testUsername",
    email     = "test@email.com",
    emailVerified = true,
    profile = None,
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  "setupTOTPMFA" should {
    "return a MFATOTPQR" when {
      "a QR code has been generated for the user" in {
        mockGenerateSecret(resp = Secret("testSecret"))
        mockGenerateQRCode(resp = QRCode("testQRData"))

        awaitAndAssert(testOrchestrator.setupTOTPMFA(testUser.id)) {
          _ mustBe MFATOTPQR("testQRData")
        }
      }
    }

    "QRGenerationFailed" when {
      "there was a problem building the code" in {
        mockGenerateSecret(resp = Secret("testSecret"))
        mockGenerateQRCode(resp = QRCodeFailed)

        awaitAndAssert(testOrchestrator.setupTOTPMFA(testUser.id)) {
          _ mustBe QRGenerationFailed
        }
      }
    }

    "SecretGenerationFailed" when {
      "there was a problem generating the secret" in {
        mockGenerateSecret(resp = FailedGeneration)

        awaitAndAssert(testOrchestrator.setupTOTPMFA(testUser.id)) {
          _ mustBe SecretGenerationFailed
        }
      }
    }
  }

  "isMFAEnabled" should {
    "return true" when {
      "the user has MFA enabled on their account" in {
        mockGetMFAStatus(resp = true)

        awaitAndAssert(testOrchestrator.isMFAEnabled(testUser.id)) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "the user does not have MFA enabled on their account" in {
        mockGetMFAStatus(resp = false)

        awaitAndAssert(testOrchestrator.isMFAEnabled(testUser.id)) {
          _ mustBe false
        }
      }
    }
  }

  "disableMFA" should {
    "return true" when {
      "the users MFA has been disabled" in {
        mockRemoveTOTPMFA(removed = true)

        awaitAndAssert(testOrchestrator.disableMFA(testUser.id)) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "there was a problem disabling the users MFA" in {
        mockRemoveTOTPMFA(removed = false)

        awaitAndAssert(testOrchestrator.disableMFA(testUser.id)) {
          _ mustBe false
        }
      }
    }
  }
}
