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

package services

import java.util.UUID

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.obfuscation.Obfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import dev.samstevens.totp.code.{CodeVerifier, HashingAlgorithm}
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.SecretGenerator
import helpers.Assertions
import helpers.database.{MockIndividualStore, MockOrganisationStore}
import helpers.misc.MockJavaTOTP
import models.User
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class TOTPServiceSpec
  extends PlaySpec
    with Assertions
    with MockIndividualStore
    with MockOrganisationStore
    with MockJavaTOTP
    with Obfuscators
    with SecurityConfiguration {

  override val locale: String = "models.User"

  private val testService: TOTPService = new TOTPService {
    override val secretGenerator: SecretGenerator = mockSecretGenerator
    override val qrGenerator: ZxingPngQrGenerator = mockQrGenerator
    override val codeVerifier: CodeVerifier = mockCodeVerifier
    override val algorithm: HashingAlgorithm = HashingAlgorithm.SHA512
    override val individualUserStore: IndividualUserStore = mockIndividualStore
    override val organisationUserStore: OrganisationUserStore = mockOrganisationStore
    override val mfaIssuer: String = "testIssuer"
    override val mfaDigits: Int = 6
    override val mfaPeriod: Int = 30
  }

  val now = DateTime.now()

  val monthInt = now.getMonthOfYear
  val month = f"$monthInt%02d"

  val nowString = s"${now.getYear}-${month}-${now.getDayOfMonth}"

  val testUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUserName".encrypt,
    email     = "test@email.com".encrypt,
    emailVerified = true,
    profile = None,
    address = None,
    accType   = "individual",
    password  = "testPassword",
    authorisedClients = List(),
    salt      = "testSalt",
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  "generateSecret" should {
    "return a Secret" when {
      "the secret was generated and the secret has been saved against the user" in {
        mockGenerateSecret()
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.generateSecret(testUser.id)) {
          _ mustBe Secret("testSecret")
        }
      }
    }

    "return a FailedGeneration" when {
      "the secret was generated but the secret has not been saved against the user" in {
        mockGenerateSecret()
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.generateSecret(testUser.id)) {
          _ mustBe FailedGeneration
        }
      }
    }
  }

  "getCurrentSecret" should {
    "return a Secret" when {
      "the user has an MFA secret" in {
        mockIndividualValidateUserOn(user = Some(testUser.copy(mfaSecret = Some("testSecret"))))

        awaitAndAssert(testService.getCurrentSecret(testUser.id)) {
          _ mustBe Secret("testSecret")
        }
      }
    }

    "return a FailedGeneration" when {
      "the user doesn't have an MFA secret" in {
        mockIndividualValidateUserOn(user = Some(testUser.copy(mfaSecret = None)))

        awaitAndAssert(testService.getCurrentSecret(testUser.id)) {
          _ mustBe FailedGeneration
        }
      }

      "the user doesn't exist" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.getCurrentSecret(testUser.id)) {
          _ mustBe FailedGeneration
        }
      }
    }
  }

  "getMFAStatus" should {
    "return true" when {
      "the user has MFA enabled" in {
        mockIndividualValidateUserOn(user = Some(testUser.copy(mfaEnabled = true)))

        awaitAndAssert(testService.getMFAStatus(testUser.id)) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "the user doesn't have MFA enabled" in {
        mockIndividualValidateUserOn(user = Some(testUser.copy(mfaEnabled = false)))

        awaitAndAssert(testService.getMFAStatus(testUser.id)) {
          _ mustBe false
        }
      }

      "the user doesn't exist" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.getMFAStatus(testUser.id)) {
          _ mustBe false
        }
      }
    }
  }

  "enableAccountMFA" should {
    "return an MFAEnabled" when {
      "the users MFA has been enabled" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.enableAccountMFA(testUser.id)) {
          _ mustBe MFAEnabled
        }
      }
    }

    "return an MFADisabled" when {
      "there was a problem enabling MFA for the user" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.enableAccountMFA(testUser.id)) {
          _ mustBe MFADisabled
        }
      }
    }
  }

  "validateCodes" should {
    "return true" when {
      "the code is valid" in {
        mockIsValidCode(isValid = true)

        assertOutput(testService.validateCodes("testSecret", "testCode")) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "the code is not valid" in {
        mockIsValidCode(isValid = false)

        assertOutput(testService.validateCodes("testSecret", "testCode")) {
          _ mustBe false
        }
      }
    }
  }

  "removeTOTPMFA" should {
    "return true" when {
      "the users MFA has been disabled" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.removeTOTPMFA(testUser.id)) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "there was a problem disabling the users MFA" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.removeTOTPMFA(testUser.id)) {
          _ mustBe false
        }
      }
    }
  }
}
