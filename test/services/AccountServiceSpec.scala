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

import java.time.Instant
import java.util.{Date, UUID}

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.obfuscation.Obfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import helpers.Assertions
import helpers.database.{MockIndividualStore, MockOrganisationStore}
import models.{User, UserInfo}
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class AccountServiceSpec
  extends PlaySpec
    with Assertions
    with MockIndividualStore
    with MockOrganisationStore
    with Obfuscators
    with SecurityConfiguration {

  override val locale: String = "models.User"

  private val testService: AccountService = new AccountService {
    override val userStore: IndividualUserStore = mockIndividualStore
    override val orgUserStore: OrganisationUserStore = mockOrganisationStore
  }

  val now = DateTime.now()

  val monthInt = now.getMonthOfYear
  val month = f"$monthInt%02d"

  val nowString = s"${now.getYear}-${month}-${now.getDayOfMonth}"

  val testIndividualUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUserName".encrypt,
    email     = "test@email.com".encrypt,
    accType   = "individual",
    password  = "testPassword",
    authorisedClients = None,
    salt      = "testSalt",
    createdAt = now
  )

  val testOrganisationUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUserName".encrypt,
    email     = "test@email.com".encrypt,
    accType   = "organisation",
    password  = "testPassword",
    authorisedClients = None,
    salt      = "testSalt",
    createdAt = now
  )

  "getIndividualAccountInfo" should {
    "return an populated map" when {
      "the specified user was found" in {
        mockIndividualProjectValue(value = Map(
          "id"        -> bson.BsonString(testIndividualUser.id),
          "userName"  -> bson.BsonString(testIndividualUser.userName),
          "email"     -> bson.BsonString(testIndividualUser.email),
          "createdAt" -> bson.BsonDateTime(Date.from(Instant.ofEpochMilli(now.getMillis))),
        ))

        awaitAndAssert(testService.getIndividualAccountInfo(testIndividualUser.id)) {
          _ mustBe Some(UserInfo(
            id = testIndividualUser.id,
            userName = "testUserName",
            email = "test@email.com",
            accType = testIndividualUser.accType,
            authorisedClients = List.empty[String],
            createdAt = now.withZone(DateTimeZone.UTC)
          ))
        }
      }
    }

    "return an empty map" when {
      "the specified user wasn't found" in {
        mockIndividualProjectValue(value = Map())

        awaitAndAssert(testService.getIndividualAccountInfo(testIndividualUser.id)) {
          _ mustBe None
        }
      }
    }
  }

  "getOrganisationAccountInfo" should {
    "return an populated map" when {
      "the specified user was found" in {
        mockOrganisationProjectValue(value = Map(
          "id"        -> bson.BsonString(testOrganisationUser.id),
          "userName"  -> bson.BsonString(testOrganisationUser.userName),
          "email"     -> bson.BsonString(testOrganisationUser.email),
          "createdAt" -> bson.BsonDateTime(Date.from(Instant.ofEpochMilli(now.getMillis))),
        ))

        awaitAndAssert(testService.getOrganisationAccountInfo(testOrganisationUser.id)) {
          _ mustBe Some(UserInfo(
            id = testOrganisationUser.id,
            userName = "testUserName",
            email = "test@email.com",
            accType = testOrganisationUser.accType,
            authorisedClients = List.empty[String],
            createdAt = now.withZone(DateTimeZone.UTC)
          ))
        }
      }
    }

    "return an empty map" when {
      "the specified user wasn't found" in {
        mockOrganisationProjectValue(value = Map())

        awaitAndAssert(testService.getOrganisationAccountInfo(testOrganisationUser.id)) {
          _ mustBe None
        }
      }
    }
  }

  "determineAccountTypeFromId" should {
    "return individual" when {
      (0 to 10)
        .map(_ => s"user-${UUID.randomUUID()}")
        .foreach { id =>
          s"given $id as the id" in {
            assertOutput(testService.determineAccountTypeFromId(id)) {
              _ mustBe Some("individual")
            }
          }
        }
    }

    "return organisation" when {
      (0 to 10)
        .map(_ => s"org-user-${UUID.randomUUID()}")
        .foreach { id =>
          s"given $id as the id" in {
            assertOutput(testService.determineAccountTypeFromId(id)) {
              _ mustBe Some("organisation")
            }
          }
        }
    }

    "return Left" when {
      (0 to 10)
        .map(_ => UUID.randomUUID().toString)
        .foreach { id =>
          s"given ${id} as the id" in {
            assertOutput(testService.determineAccountTypeFromId(id)) {
              _ mustBe None
            }
          }
        }
    }
  }

  "linkAuthorisedClientTo" should {
    "return a LinkSuccess" when {
      "the individual user has been linked to a client" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.linkAuthorisedClientTo(testIndividualUser.id, "testAppId")) {
          _ mustBe LinkSuccess
        }
      }

      "the organisation user has been linked to a client" in {
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))
        mockUpdateOrgUser(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.linkAuthorisedClientTo(testOrganisationUser.id, "testAppId")) {
          _ mustBe LinkSuccess
        }
      }
    }

    "return a LinkFailed" when {
      "the individual user hasn't been linked to a client" in {
        mockIndividualValidateUserOn(user = Some(testIndividualUser))
        mockUpdateIndUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.linkAuthorisedClientTo(testIndividualUser.id, "testAppId")) {
          _ mustBe LinkFailed
        }
      }

      "the organisation user hasn't been linked to a client" in {
        mockOrganisationValidateUserOn(user = Some(testOrganisationUser))
        mockUpdateOrgUser(resp = MongoFailedUpdate)

        awaitAndAssert(testService.linkAuthorisedClientTo(testOrganisationUser.id, "testAppId")) {
          _ mustBe LinkFailed
        }
      }
    }

    "return a NoUserFound" when {
      "the individual user doesn't exist" in {
        mockIndividualValidateUserOn(user = None)

        awaitAndAssert(testService.linkAuthorisedClientTo(testIndividualUser.id, "testAppId")) {
          _ mustBe NoUserFound
        }
      }

      "the organisation user doesn't exist" in {
        mockOrganisationValidateUserOn(user = None)

        awaitAndAssert(testService.linkAuthorisedClientTo(testOrganisationUser.id, "testAppId")) {
          _ mustBe NoUserFound
        }
      }

      "the user Id is invalid" in {
        awaitAndAssert(testService.linkAuthorisedClientTo("invalid-user-id", "testAppId")) {
          _ mustBe NoUserFound
        }
      }
    }
  }
}
