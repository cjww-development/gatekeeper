/*
 * Copyright 2021 CJWW Development
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

package services.users

import database.{AppStore, UserStore}
import dev.cjww.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import dev.cjww.security.Implicits._
import dev.cjww.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.database.{MockAppStore, MockIndividualStore, MockOrganisationStore}
import models.{DigitalContact, Email, RegisteredApplication, User}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import utils.StringUtils

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec
  extends PlaySpec
    with Assertions
    with Obfuscators
    with MockIndividualStore
    with MockOrganisationStore
    with MockAppStore {

  override val locale: String = ""

  private val testService: RegistrationService = new RegistrationService {
    override protected val individualUserStore: UserStore = mockIndividualStore
    override protected val organisationUserStore: UserStore = mockOrganisationStore
    override val appStore: AppStore = mockAppStore
  }

  val testIndividualUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUserName",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com",
        verified = true
      ),
      phone = None
    ),
    profile = None,
    address = None,
    accType   = "individual",
    password  = "testPassword",
    authorisedClients = List(),
    salt      = "testSalt",
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  val testOrganisationUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUserName",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com",
        verified = true
      ),
      phone = None
    ),
    profile = None,
    address = None,
    accType   = "organisation",
    password  = "testPassword",
    authorisedClients = List(),
    salt      = "testSalt",
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    iconUrl      = None,
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId".encrypt,
    clientSecret = Some("testSecret".encrypt),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = DateTime.now()
  )

  "createNewUser" should {
    "return a MongoSuccessCreate" when {
      "an individual user has been created" in {
        mockCreateIndividualUser(success = true)

        awaitAndAssert(testService.createNewUser(testIndividualUser)) {
          _ mustBe MongoSuccessCreate
        }
      }

      "an organisation user has been created" in {
        mockCreateOrgUser(success = true)

        awaitAndAssert(testService.createNewUser(testOrganisationUser)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoFailedCreate" when {
      "there was a problem creating an individual user" in {
        mockCreateIndividualUser(success = false)

        awaitAndAssert(testService.createNewUser(testIndividualUser)) {
          _ mustBe MongoFailedCreate
        }
      }

      "there was a problem creating an organisation user" in {
        mockCreateOrgUser(success = false)

        awaitAndAssert(testService.createNewUser(testOrganisationUser)) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "isIdentifierInUse" should {
    "return true" when {
      "the identifier in use on an individual user" in {
        mockMultipleIndividualValidateUserOn(
          userOne = Some(testIndividualUser),
          userTwo = None
        )

        mockMultipleOrganisationValidateUserOn(
          userOne = None,
          userTwo = None
        )

        awaitAndAssert(testService.isIdentifierInUse(testIndividualUser.digitalContact.email.address, testIndividualUser.userName)) {
          res => assert(res)
        }
      }

      "the identifier in use on an organisation user" in {
        mockMultipleIndividualValidateUserOn(
          userOne = None,
          userTwo = None
        )

        mockMultipleOrganisationValidateUserOn(
          userOne = Some(testOrganisationUser),
          userTwo = None
        )

        awaitAndAssert(testService.isIdentifierInUse(testIndividualUser.digitalContact.email.address, testIndividualUser.userName)) {
          res => assert(res)
        }
      }

      "the identifier in in use on both user types" in {
        mockMultipleIndividualValidateUserOn(
          userOne = Some(testIndividualUser),
          userTwo = None
        )

        mockMultipleOrganisationValidateUserOn(
          userOne = Some(testOrganisationUser),
          userTwo = None
        )

        awaitAndAssert(testService.isIdentifierInUse(testIndividualUser.digitalContact.email.address, testIndividualUser.userName)) {
          res => assert(res)
        }
      }
    }

    "return false" when {
      "neither user type is using the identifier" in {
        mockMultipleIndividualValidateUserOn(
          userOne = None,
          userTwo = None
        )

        mockMultipleOrganisationValidateUserOn(
          userOne = None,
          userTwo = None
        )

        awaitAndAssert(testService.isIdentifierInUse(testIndividualUser.digitalContact.email.address, testIndividualUser.userName)) {
          res => assert(!res)
        }
      }
    }
  }

  "revalidateSalt" should {
    "return the original salt" when {
      "the original salt isn't already been used on another account" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = None)

        val salt = StringUtils.salter(length = 8)

        awaitAndAssert(testService.validateSalt(salt)) {
          _ mustBe salt
        }
      }
    }

    "return a new salt string" when {
      "the new users salt is already being used on another account" in {
        mockMultipleIndividualValidateUserOn(
          userOne = Some(testIndividualUser),
          userTwo = None
        )

        mockMultipleOrganisationValidateUserOn(
          userOne = Some(testOrganisationUser),
          userTwo = None
        )

        awaitAndAssert(testService.validateSalt(testIndividualUser.salt)) { res =>
          assert(res != testIndividualUser.salt)
          assert(res != testOrganisationUser.salt)
        }
      }
    }
  }

  "createApp" should {
    "return a MongoCreateSuccess" when {
      "the app was created" in {
        mockCreateApp(success = true)

        awaitAndAssert(testService.createApp(testApp)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoCreateFailed" when {
      "there was a problem creating the app" in {
        mockCreateApp(success = false)

        awaitAndAssert(testService.createApp(testApp)) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "validateIdsAndSecrets" should {
    "return a new app" when {
      "both the id and secret is in use" in {
        mockMultipleValidateAppOn(
          appOne = Some(testApp),
          appTwo = Some(testApp),
          appThree = None,
          appFour = None
        )

        awaitAndAssert(testService.validateIdsAndSecrets(testApp)) { res =>
          assert(res != testApp)
        }
      }

      "just the id is in use" in {
        mockMultipleValidateAppOn(
          appOne = Some(testApp),
          appTwo = None,
          appThree = None,
          appFour = None
        )

        awaitAndAssert(testService.validateIdsAndSecrets(testApp)) { res =>
          assert(res != testApp)
        }
      }

      "just the id is in use but the client is public" in {
        mockMultipleValidateAppOn(
          appOne = Some(testApp),
          appTwo = None,
          appThree = None,
          appFour = None
        )

        awaitAndAssert(testService.validateIdsAndSecrets(testApp)) { res =>
          assert(res != testApp)
        }
      }

      "just the secret is in use" in {
        mockMultipleValidateAppOn(
          appOne = None,
          appTwo = Some(testApp),
          appThree = None,
          appFour = None
        )

        awaitAndAssert(testService.validateIdsAndSecrets(testApp)) { res =>
          assert(res != testApp)
        }
      }
    }

    "return the original id and secret" when {
      "neither the client Id or secret are in use" in {
        mockMultipleValidateAppOn(
          appOne = None,
          appTwo = None,
          appThree = None,
          appFour = None
        )

        awaitAndAssert(testService.validateIdsAndSecrets(testApp)) {
          _ mustBe testApp
        }
      }
    }
  }
}
