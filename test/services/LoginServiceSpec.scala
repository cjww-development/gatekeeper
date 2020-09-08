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

import database.{IndividualUserStore, OrganisationUserStore}
import helpers.Assertions
import helpers.database.{MockAppStore, MockIndividualStore, MockOrganisationStore}
import models.User
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonString
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class LoginServiceSpec
  extends PlaySpec
    with Assertions
    with MockIndividualStore
    with MockOrganisationStore
    with MockAppStore {

  private val testService: LoginService = new LoginService {
    override val userStore: IndividualUserStore = mockIndividualStore
    override val orgUserStore: OrganisationUserStore = mockOrganisationStore
  }

  val testIndividualUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUserName",
    email     = "test@email.com",
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
    email     = "test@email.com",
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = DateTime.now()
  )

  "getUserSalt" should {
    "return the users salt" when {
      "the user is found in the individual store on user name" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id")),
          valueTwo = Map()
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        awaitAndAssert(testService.getUserSalt("testUserName")) {
          _ mustBe Some("testSalt")
        }
      }

      "the user is found in the individual store on email" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id"))
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        awaitAndAssert(testService.getUserSalt("testUserName")) {
          _ mustBe Some("testSalt")
        }
      }

      "the user is found in the organisation store on user name" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id")),
          valueTwo = Map()
        )

        awaitAndAssert(testService.getUserSalt("testUserName")) {
          _ mustBe Some("testSalt")
        }
      }

      "the user is found in the organisation store on email" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id"))
        )

        awaitAndAssert(testService.getUserSalt("testUserName")) {
          _ mustBe Some("testSalt")
        }
      }
    }

    "return none" when {
      "a user cannot be found in either user store" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map()
        )

        awaitAndAssert(testService.getUserSalt("testUserName")) {
          _ mustBe None
        }
      }
    }

    "throw an exception" when {
      "the user has been found in both user stores" in {
        mockIndividualMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id"))
        )

        mockOrganisationMultipleProjectValue(
          valueOne = Map(),
          valueTwo = Map("salt" -> BsonString("testSalt"), "id" -> BsonString("id"))
        )

        awaitAndIntercept[Exception](testService.getUserSalt("testUserName"))
      }
    }
  }

  "validateUser" should {
    "return a user" when {
      "a matching individual user has been found" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockOrganisationValidateUserOn(user = None)

        awaitAndAssert(testService.validateUser(testIndividualUser.userName, testIndividualUser.password)) {
          _ mustBe Some(testIndividualUser)
        }
      }

      "a matching organisation user has been found" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))

        awaitAndAssert(testService.validateUser(testOrganisationUser.userName, testOrganisationUser.password)) {
          _ mustBe Some(testOrganisationUser)
        }
      }
    }

    "return None" when {
      "no matching user has been found" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = None)

        awaitAndAssert(testService.validateUser(testOrganisationUser.userName, testOrganisationUser.password)) {
          _ mustBe None
        }
      }
    }

    "throw an exception" when {
      "the user appears in both user stores" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))

        awaitAndIntercept[Exception](testService.getUserSalt("testUserName"))
      }
    }
  }
}
