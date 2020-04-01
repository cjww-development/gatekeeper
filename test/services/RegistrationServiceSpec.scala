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

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import database.{IndividualUserStore, OrganisationUserStore}
import helpers.database.{MockIndividualStore, MockOrganisationStore}
import models.User
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationServiceSpec
  extends PlaySpec
    with FutureAwaits
    with DefaultAwaitTimeout
    with MockIndividualStore
    with MockOrganisationStore {

  private val testService: RegistrationService = new RegistrationService {
    override val userStore: IndividualUserStore = mockIndividualStore
    override val orgUserStore: OrganisationUserStore = mockOrganisationStore
  }

  val testIndividualUser: User = User(
    id       = s"user-${UUID.randomUUID()}",
    userName = "testUserName",
    email    = "test@email.com",
    accType  = "INDIVIDUAL",
    password = "testPassword"
  )

  val testOrganisationUser: User = User(
    id       = s"org-user-${UUID.randomUUID()}",
    userName = "testUserName",
    email    = "test@email.com",
    accType  = "ORGANISATION",
    password = "testPassword"
  )

  "createNewUser" should {
    "return a MongoSuccessCreate" when {
      "an individual user has been created" in {
        mockCreateIndividualUser(success = true)

        val res = await(testService.createNewUser(testIndividualUser))
        res mustBe MongoSuccessCreate
      }

      "an organisation user has been created" in {
        mockCreateOrgUser(success = true)

        val res = await(testService.createNewUser(testOrganisationUser))
        res mustBe MongoSuccessCreate
      }
    }

    "return a MongoFailedCreate" when {
      "there was a problem creating an individual user" in {
        mockCreateIndividualUser(success = false)

        val res = await(testService.createNewUser(testIndividualUser))
        res mustBe MongoFailedCreate
      }

      "there was a problem creating an organisation user" in {
        mockCreateOrgUser(success = false)

        val res = await(testService.createNewUser(testOrganisationUser))
        res mustBe MongoFailedCreate
      }
    }
  }

  "validateEmail" should {
    "return true" when {
      "the new users email already exists in the database on either type of user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockOrganisationValidateUserOn(user = None)

        val res: Boolean = await(testService.validateEmail(testIndividualUser.email))
        res mustBe true
      }

      "the new users email already exists in the database on both types of user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))

        val res: Boolean = await(testService.validateEmail(testIndividualUser.email))
        res mustBe true
      }
    }

    "return false" when {
      "the new users email doesn't exist in the database on both types of user" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = None)

        val res: Boolean = await(testService.validateEmail(testIndividualUser.email))
        res mustBe false
      }
    }
  }

  "validateUsername" should {
    "return true" when {
      "the new users user name already exists in the database on both types of user" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))

        val res: Boolean = await(testService.validateUsername(testIndividualUser.userName))
        res mustBe true
      }

      "the new users user name already exists in the database on one type of user" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))

        val res: Boolean = await(testService.validateUsername(testIndividualUser.userName))
        res mustBe true
      }
    }

    "return false" when {
      "the new users user name doesn't exist in the database on both types of user" in {
        mockIndividualValidateUserOn(user = None)
        mockOrganisationValidateUserOn(user = None)

        val res: Boolean = await(testService.validateUsername(testIndividualUser.userName))
        res mustBe false
      }
    }
  }
}
