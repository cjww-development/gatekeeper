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
import helpers.services.{MockAccountService, MockGrantService, MockTokenService}
import models.{Grant, User}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.{AccountService, GrantService, TokenService}

import scala.concurrent.ExecutionContext.Implicits.global

class TokenOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockAccountService
    with MockGrantService
    with MockTokenService {

  val testOrchestrator: TokenOrchestrator = new TokenOrchestrator {
    override protected val grantService: GrantService = mockGrantService
    override protected val tokenService: TokenService = mockTokenService
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
    createdAt = now
  )

  val testOrgUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUsername",
    email     = "test@email.com",
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    createdAt = now
  )

  val testGrant: Grant = Grant(
    responseType = "code",
    authCode = "testAuthCode",
    scope = Seq("testScope"),
    clientId = "testClientId",
    userId = "testUserId",
    accType = "individual",
    redirectUri = "testRedirect",
    createdAt = DateTime.now()
  )

  "issueToken" should {
    "return an issued token" when {
      "the grant type is authorization_code (individual)" in {
        mockValidateGrant(grant = Some(testGrant))
        mockGetIndividualAccountInfo(value = Map("id" -> "testUserId"))
        mockCreateAccessToken()
        mockCreateIdToken()
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.issueToken("authorization_code", "testAuthCode", "testClientId", "testRedirect")) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "testScope",
            expiresIn = 900000,
            "testAccessToken",
            "testIdToken"
          )
        }
      }

      "the grant type is authorization_code (organisation)" in {
        mockValidateGrant(grant = Some(testGrant.copy(accType = "organisation")))
        mockGetOrganisationAccountInfo(value = Map("id" -> "testUserId"))
        mockCreateAccessToken()
        mockCreateIdToken()
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.issueToken("authorization_code", "testAuthCode", "testClientId", "testRedirect")) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "testScope",
            expiresIn = 900000,
            "testAccessToken",
            "testIdToken"
          )
        }
      }
    }

    "return an invalid user" when {
      "the user data could not be found" in {
        mockValidateGrant(grant = Some(testGrant))
        mockGetIndividualAccountInfo(value = Map())
        mockCreateAccessToken()
        mockCreateIdToken()
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.issueToken("authorization_code", "testAuthCode", "testClientId", "testRedirect")) {
          _ mustBe InvalidUser
        }
      }
    }

    "return an invalid grant" when {
      "the grant could not be validated" in {
        mockValidateGrant(grant = None)

        awaitAndAssert(testOrchestrator.issueToken("authorization_code", "testAuthCode", "testClientId", "testRedirect")) {
          _ mustBe InvalidGrant
        }
      }
    }

    "return an invalid grant type" when {
      "the grant type is not recognised" in {
        awaitAndAssert(testOrchestrator.issueToken("invalid-grant", "testAuthCode", "testClientId", "testRedirect")) {
          _ mustBe InvalidGrantType
        }
      }
    }
  }
}
