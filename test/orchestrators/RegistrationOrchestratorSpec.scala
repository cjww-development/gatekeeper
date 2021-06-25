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

import utils.StringUtils._
import helpers.Assertions
import helpers.services.{MockEmailService, MockPhoneService, MockRegistrationService, MockUserService}
import models._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.comms.{EmailService, PhoneService}
import services.users.{RegistrationService, UserService}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockRegistrationService
    with MockEmailService
    with MockPhoneService
    with MockUserService {

  val testOrchestrator: RegistrationOrchestrator = new RegistrationOrchestrator {
    override protected val phoneService: PhoneService = mockPhoneService
    override protected val userService: UserService = mockUserService
    override val registrationService: RegistrationService = mockRegistrationService
    override protected val emailService: EmailService = mockEmailService
  }

  val testUser: User = User(
    id        = "testId",
    userName  = "testUsername",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com".encrypt,
        verified = true
      ),
      phone = None
    ),
    profile = None,
    address = None,
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId",
    clientSecret = Some("testSecret"),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = DateTime.now()
  )

  "registerUser" should {
    implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    "return a Registered response" when {
      "the user is successfully registered" in {
        mockIsIdentifierInUse(inUse = false)
        mockValidateSalt(salt = "testSalt")
        mockCreateNewUser(success = true)
        mockSaveVerificationRecord(verificationRecord = Verification(
          verificationId = "testVerifyId",
          userId = "testUserId",
          contactType = "email",
          contact = "test@email.com",
          code = None,
          accType = "individual",
          createdAt = new DateTime()
        ))
        mockSendEmailVerificationMessage()

        awaitAndAssert(testOrchestrator.registerUser(testUser)) {
          _ mustBe Registered
        }
      }
    }

    "return an AccountIdsInUse response" when {
      "the new users email and user name are already in use" in {
        mockIsIdentifierInUse(inUse = true)

        awaitAndAssert(testOrchestrator.registerUser(testUser)) {
          _ mustBe AccountIdsInUse
        }
      }
    }

    "return a RegistrationError response" when {
      "there was a problem registering the new user" in {
        mockIsIdentifierInUse(inUse = false)
        mockValidateSalt(salt = "testSalt")
        mockCreateNewUser(success = false)

        awaitAndAssert(testOrchestrator.registerUser(testUser)) {
          _ mustBe RegistrationError
        }
      }
    }
  }

  "registerApplication" should {
    "return an AppRegistered response" when {
      "the app has been registered" in {
        mockValidateIdsAndSecrets(app = testApp)

        mockCreateApp(success = true)

        awaitAndAssert(testOrchestrator.registerApplication(testApp)) {
          _ mustBe AppRegistered
        }
      }
    }

    "return an AppRegisteredError response" when {
      "there was a problem registering the app" in {
        mockValidateIdsAndSecrets(app = testApp)

        mockCreateApp(success = false)

        awaitAndAssert(testOrchestrator.registerApplication(testApp)) {
          _ mustBe AppRegistrationError
        }
      }
    }
  }
}
