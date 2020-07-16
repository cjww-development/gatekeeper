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

import com.cjwwdev.security.SecurityConfiguration
import com.cjwwdev.security.obfuscation.Obfuscators
import com.cjwwdev.security.Implicits._
import database.{IndividualUserStore, OrganisationUserStore}
import helpers.Assertions
import helpers.database.{MockIndividualStore, MockOrganisationStore}
import models.User
import org.joda.time.DateTime
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
    salt      = "testSalt",
    createdAt = now
  )

  val testOrganisationUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUserName".encrypt,
    email     = "test@email.com".encrypt,
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    createdAt = now
  )

  "getIndividualAccountInfo" should {
    "return an populated map" when {
      "the specified user was found" in {
        mockIndividualProjectValue(value = Map(
          "userName"  -> bson.BsonString(testOrganisationUser.userName),
          "email"     -> bson.BsonString(testOrganisationUser.email),
          "createdAt" -> bson.BsonDateTime(Date.from(Instant.ofEpochMilli(now.getMillis))),
        ))

        awaitAndAssert(testService.getIndividualAccountInfo(testIndividualUser.id)) {
          _ mustBe Map(
            "userName"  -> "testUserName",
            "email"     -> "test@email.com",
            "createdAt" -> nowString
          )
        }
      }
    }

    "return an empty map" when {
      "the specified user wasn't found" in {
        mockIndividualProjectValue(value = Map())

        awaitAndAssert(testService.getIndividualAccountInfo(testIndividualUser.id)) {
          _ mustBe Map()
        }
      }
    }
  }

  "getOrganisationAccountInfo" should {
    "return an populated map" when {
      "the specified user was found" in {
        mockOrganisationProjectValue(value = Map(
          "userName"  -> bson.BsonString(testOrganisationUser.userName),
          "email"     -> bson.BsonString(testOrganisationUser.email),
          "createdAt" -> bson.BsonDateTime(Date.from(Instant.ofEpochMilli(now.getMillis))),
        ))

        awaitAndAssert(testService.getOrganisationAccountInfo(testOrganisationUser.id)) {
          _ mustBe Map(
            "userName"  -> "testUserName",
            "email"     -> "test@email.com",
            "createdAt" -> nowString
          )
        }
      }
    }

    "return an empty map" when {
      "the specified user wasn't found" in {
        mockOrganisationProjectValue(value = Map())

        awaitAndAssert(testService.getOrganisationAccountInfo(testOrganisationUser.id)) {
          _ mustBe Map()
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
              _ mustBe Right("individual")
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
              _ mustBe Right("organisation")
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
              _ mustBe Left(())
            }
          }
        }
    }
  }
}
