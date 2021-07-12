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

import database.UserStore
import dev.cjww.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import helpers.Assertions
import helpers.database.{MockAppStore, MockIndividualStore, MockLoginAttemptStore, MockOrganisationStore}
import models._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import utils.StringUtils

import java.util.{Date, UUID}
import scala.concurrent.ExecutionContext.Implicits.global

class UserServiceSpec
  extends PlaySpec
    with Assertions
    with MockIndividualStore
    with MockOrganisationStore
    with MockLoginAttemptStore
    with MockAppStore {

  private val testService: UserService = new UserService {
    override protected val individualUserStore: UserStore = mockIndividualStore
    override protected val organisationUserStore: UserStore = mockOrganisationStore
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
    salt      = "testSalt",
    authorisedClients = List(),
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
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  "getUserInfo" should {
    "return a UserInfo" when {
      "a user a has been found and the user data has been mapped to a UserInfo" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))

        awaitAndAssert(testService.getUserInfo("user-testUserId")) {
          _ mustBe Some(UserInfo.fromUser(testIndividualUser))
        }
      }
    }

    "return None" when {
      "no user has been found" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.getUserInfo("user-testUserId")) {
          _ mustBe None
        }
      }
    }
  }

  "linkClientToUser" should {
    "return a LinkSuccess" when {
      "the client has been linked to the user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.linkClientToUser("user-testUserId", "testAppId", Seq("testScope"))) {
          _ mustBe LinkSuccess
        }
      }
    }

    "return a LinkFailed" when {
      "there was a problem linking the client to the user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.linkClientToUser("user-testUserId", "testAppId", Seq("testScope"))) {
          _ mustBe LinkFailed
        }
      }
    }

    "return a NoUserFound" when {
      "no user could be found" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.linkClientToUser("user-testUserId", "testAppId", Seq("testScope"))) {
          _ mustBe NoUserFound
        }
      }
    }
  }

  "unlinkClientFromUser" should {
    "return a LinkSuccess" when {
      "the client has been unlinked from the user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.unlinkClientFromUser("user-testUserId", "testAppId")) {
          _ mustBe LinkSuccess
        }
      }
    }

    "return a LinkFailed" when {
      "there was a problem linking the client to the user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.unlinkClientFromUser("user-testUserId", "testAppId")) {
          _ mustBe LinkFailed
        }
      }
    }

    "return a NoUserFound" when {
      "no user could be found" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.unlinkClientFromUser("user-testUserId", "testAppId")) {
          _ mustBe NoUserFound
        }
      }
    }
  }

  "setEmailVerifiedStatus" should {
    "return a MongoSuccessUpdate" when {
      "the users email verification status has been set to true" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.setEmailVerifiedStatus("user-testUserId", verified = true)) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users email verification status has been set to false" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.setEmailVerifiedStatus("user-testUserId", verified = true)) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updateUserEmailAddress" should {
    "return a MongoSuccessUpdate" when {
      "the users email has been updated" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateUserEmailAddress("user-testUserId", "testEmailAddress")) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users email has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateUserEmailAddress("user-testUserId", "testEmailAddress")) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updatePassword" should {
    "return a MongoSuccessUpdate" when {
      "the users password and salt has been updated" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updatePassword("user-testUserId", "testPass", "testSalt")) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users password and salt has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updatePassword("user-testUserId", "testPass", "testSalt")) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updateName" should {
    "return a MongoSuccessUpdate" when {
      "the users name has been updated" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateName("user-testUserId", Some("first"), Some("middle"), Some("last"), Some("nickname"))) {
          _ mustBe MongoSuccessUpdate
        }
      }

      "the users name is not provider" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateName("user-testUserId", None, None, None, None)) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users name has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateName("user-testUserId", Some("first"), Some("middle"), Some("last"), Some("nickname"))) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updateGender" should {
    "return a MongoSuccessUpdate" when {
      "the users gender has been updated" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateGender("user-testUserId", "testGender")) {
          _ mustBe MongoSuccessUpdate
        }
      }

      "the users gender has been unset" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateGender("user-testUserId", "not specified")) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users gender has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateGender("user-testUserId", "testGender")) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updateBirthday" should {
    "return a MongoSuccessUpdate" when {
      "the users birthday has been updated" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateBirthday("user-testUserId", Some(new Date()))) {
          _ mustBe MongoSuccessUpdate
        }
      }

      "the users birthday has been unset" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateBirthday("user-testUserId", None)) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users birthday has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateBirthday("user-testUserId", None)) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "updateAddress" should {
    "return a MongoSuccessUpdate" when {
      "the users address has been updated" in {
        val testAddress = Address(
          formatted = "testFormatted",
          streetAddress = "testStreetAddress",
          locality = "testLocality",
          region = "testRegion",
          postalCode = "testPostcode",
          country = "testCountry"
        )

        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateAddress("user-testUserId", Some(testAddress))) {
          _ mustBe MongoSuccessUpdate
        }
      }

      "the users address has been unset" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateAddress("user-testUserId", None)) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users address has not been updated" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateAddress("user-testUserId", None)) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "setVerifiedPhoneNumber" should {
    "return a MongoSuccessUpdate" when {
      "the users phone verification status has been set to true" in {
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.setVerifiedPhoneNumber("user-testUserId", "testPhoneNumber")) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the users phone verification status has been set to false" in {
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.setVerifiedPhoneNumber("user-testUserId", "testPhoneNumber")) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }

  "validateCurrentPassword" should {
    "return true" when {
      "the users password has been validated" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser.copy(
          salt = "testSalt",
          password = StringUtils.hasher("testSalt", "testPassword")
        )))

        awaitAndAssert(testService.validateCurrentPassword("user-testUserId", "testPassword")) {
          _ mustBe true
        }
      }
    }

    "return false" when {
      "the users password has not been validated" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))

        awaitAndAssert(testService.validateCurrentPassword("user-testUserId", "testCurrentPassword")) {
          _ mustBe false
        }
      }

      "the user could not be found" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.validateCurrentPassword("user-testUserId", "testCurrentPassword")) {
          _ mustBe false
        }
      }
    }
  }
}
