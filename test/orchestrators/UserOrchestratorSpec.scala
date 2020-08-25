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
import helpers.services.MockAccountService
import models.{RegisteredApplication, User, UserInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.AccountService

import scala.concurrent.ExecutionContext.Implicits.global

class UserOrchestratorSpec extends PlaySpec with Assertions with MockAccountService {

  val testOrchestrator: UserOrchestrator = new UserOrchestrator {
    override protected val accountService: AccountService = mockAccountService
  }

  val now: DateTime = DateTime.now()

  val monthInt: Int = now.getMonthOfYear
  val month = f"$monthInt%02d"

  val nowString = s"${now.getYear}-${month}-${now.getDayOfMonth}"

  val testIndUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUsername",
    email     = "test@email.com",
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    createdAt = now
  )

  val testOrgUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUsername",
    email     = "test@email.com",
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    createdAt = now
  )

  "getUserDetails" should {
    "return an empty map" when {
      "the userId is invalid" in {
        mockDetermineAccountTypeFromId(value = None)

        awaitAndAssert(testOrchestrator.getUserDetails(UUID.randomUUID().toString)) {
          _ mustBe None
        }
      }

      "the individual user cannot be found" in {
        mockDetermineAccountTypeFromId(value = Some("individual"))

        mockGetIndividualAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getUserDetails(testIndUser.id)) {
          _ mustBe None
        }
      }

      "the organisation user cannot be found" in {
        mockDetermineAccountTypeFromId(value = Some("organisation"))

        mockGetOrganisationAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getUserDetails(testOrgUser.id)) {
          _ mustBe None
        }
      }
    }

    "return a populated map" when {
      "the individual user was found" in {
        mockDetermineAccountTypeFromId(value = Some("individual"))

        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = testIndUser.id,
          userName = testIndUser.userName,
          email = testIndUser.email,
          accType = testIndUser.accType,
          authorisedClients = List.empty[String],
          createdAt = now
        )))

        awaitAndAssert(testOrchestrator.getUserDetails(testIndUser.id)) {
          _ mustBe Some(UserInfo(
            id = testIndUser.id,
            userName = testIndUser.userName,
            email = testIndUser.email,
            accType = testIndUser.accType,
            authorisedClients = List.empty[String],
            createdAt = now
          ))
        }
      }

      "the organisation user was found" in {
        mockDetermineAccountTypeFromId(value = Some("organisation"))

        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testOrgUser.id,
          userName = testOrgUser.userName,
          email = testOrgUser.email,
          accType = testOrgUser.accType,
          authorisedClients = List.empty[String],
          createdAt = now
        )))

        awaitAndAssert(testOrchestrator.getUserDetails(testOrgUser.id)) {
          _ mustBe Some(UserInfo(
            id = testOrgUser.id,
            userName = testOrgUser.userName,
            email = testOrgUser.email,
            accType = testOrgUser.accType,
            authorisedClients = List.empty[String],
            createdAt = now
          ))
        }
      }
    }
  }
}
