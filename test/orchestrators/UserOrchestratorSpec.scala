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
import models.{AuthorisedClient, User, UserInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.UserService

import scala.concurrent.ExecutionContext.Implicits.global

class UserOrchestratorSpec extends PlaySpec with Assertions with MockAccountService {

  val testOrchestrator: UserOrchestrator = new UserOrchestrator {
    override protected val userService: UserService = mockAccountService
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
    mfaSecret = None,
    mfaEnabled = false,
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
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  "getUserDetails" should {
    "return an empty map" when {
      "the individual user cannot be found" in {
        mockGetIndividualAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getUserDetails(testIndUser.id)) {
          _ mustBe None
        }
      }

      "the organisation user cannot be found" in {
        mockGetOrganisationAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getUserDetails(testOrgUser.id)) {
          _ mustBe None
        }
      }
    }

    "return a populated map" when {
      "the individual user was found" in {
        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = testIndUser.id,
          userName = testIndUser.userName,
          email = testIndUser.email,
          accType = testIndUser.accType,
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        awaitAndAssert(testOrchestrator.getUserDetails(testIndUser.id)) {
          _ mustBe Some(UserInfo(
            id = testIndUser.id,
            userName = testIndUser.userName,
            email = testIndUser.email,
            accType = testIndUser.accType,
            authorisedClients = List.empty[AuthorisedClient],
            mfaEnabled = false,
            createdAt = now
          ))
        }
      }

      "the organisation user was found" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testOrgUser.id,
          userName = testOrgUser.userName,
          email = testOrgUser.email,
          accType = testOrgUser.accType,
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        awaitAndAssert(testOrchestrator.getUserDetails(testOrgUser.id)) {
          _ mustBe Some(UserInfo(
            id = testOrgUser.id,
            userName = testOrgUser.userName,
            email = testOrgUser.email,
            accType = testOrgUser.accType,
            authorisedClients = List.empty[AuthorisedClient],
            mfaEnabled = false,
            createdAt = now
          ))
        }
      }
    }
  }
}
