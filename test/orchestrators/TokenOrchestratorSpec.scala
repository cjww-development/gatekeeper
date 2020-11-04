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

import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.services._
import models.{AuthorisedClient, Grant, RegisteredApplication, Scope, User, UserInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import services.{ClientService, GrantService, ScopeService, TokenService, UserService}
import utils.BasicAuth

import scala.concurrent.ExecutionContext.Implicits.global

class TokenOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockAccountService
    with MockGrantService
    with MockClientService
    with MockTokenService
    with MockScopeService
    with Obfuscators {

  override val locale: String = ""

  val testOrchestrator: TokenOrchestrator = new TokenOrchestrator {
    override protected val grantService: GrantService = mockGrantService
    override protected val tokenService: TokenService = mockTokenService
    override protected val userService: UserService = mockAccountService
    override protected val clientService: ClientService = mockClientService
    override protected val scopeService: ScopeService = mockScopeService
    override protected val basicAuth: BasicAuth = BasicAuth
  }

  val now: DateTime = DateTime.now()

  val monthInt: Int = now.getMonthOfYear
  val month = f"$monthInt%02d"

  val nowString = s"${now.getYear}-${month}-${now.getDayOfMonth}"

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId".encrypt,
    clientSecret = Some("testSecret".encrypt),
    oauth2Flows = Seq("authorization_code", "client_credentials"),
    oauth2Scopes = Seq("openid"),
    idTokenExpiry = 900000L,
    accessTokenExpiry = 900000L,
    refreshTokenExpiry = 900000L,
    createdAt    = DateTime.now()
  )

  val testIndUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUsername",
    email     = "test@email.com",
    emailVerified = true,
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
    emailVerified = true,
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  val testGrant: Grant = Grant(
    responseType = "code",
    authCode = "testAuthCode",
    scope = Seq("openid"),
    clientId = "testClientId",
    userId = "testUserId",
    accType = "individual",
    redirectUri = "testRedirect",
    codeVerifier = None,
    codeChallenge = None,
    codeChallengeMethod = None,
    createdAt = DateTime.now()
  )

  "authorizationCodeGrant" should {
    "return an issued token" when {
      "the grant type is authorization_code (individual)" in {
        mockValidateGrant(grant = Some(testGrant))
        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = "testUserId",
          userName = "test-org",
          email = "",
          emailVerified = false,
          accType = "",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))
        mockGetScopeDetails(scopes = Seq(Scope(
          name = "openid",
          readableName = "testScope",
          desc = "test scope decs"
        )))
        mockGenerateTokenRecordSetId()
        mockCreateAccessToken()
        mockCreateIdToken()
        mockCreateTokenRecordSet(success = true)
        mockGetRegisteredAppById(app = Some(testApp))
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", "testClientId", "testRedirect", None)) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "openid",
            expiresIn = 900000,
            "testAccessToken",
            Some("testIdToken")
          )
        }
      }

      "the grant type is authorization_code (organisation)" in {
        mockValidateGrant(grant = Some(testGrant.copy(accType = "organisation")))
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = "testUserId",
          userName = "test-org",
          email = "",
          emailVerified = false,
          accType = "",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))
        mockGetScopeDetails(scopes = Seq(Scope(
          name = "openid",
          readableName = "testScope",
          desc = "test scope decs"
        )))
        mockGenerateTokenRecordSetId()
        mockCreateAccessToken()
        mockCreateIdToken()
        mockCreateTokenRecordSet(success = true)
        mockGetRegisteredAppById(app = Some(testApp))
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", "testClientId", "testRedirect", None)) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "openid",
            expiresIn = 900000,
            "testAccessToken",
            Some("testIdToken")
          )
        }
      }
    }

    "return an invalid user" when {
      "the user data could not be found" in {
        mockValidateGrant(grant = Some(testGrant))
        mockGetIndividualAccountInfo(value = None)
        mockCreateAccessToken()
        mockCreateIdToken()
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", "testClientId", "testRedirect", None)) {
          _ mustBe InvalidUser
        }
      }
    }

    "return an invalid grant" when {
      "the grant could not be validated" in {
        mockValidateGrant(grant = None)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", "testClientId", "testRedirect", None)) {
          _ mustBe InvalidGrant
        }
      }
    }
  }

  "clientCredentialsGrant" should {
    "return issued" when {
      "the client was validated" in {
        implicit val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic dGVzdElkOnRlc3RTZWNyZXQ=")

        mockGetRegisteredAppByIdAndSecret(app = Some(testApp))
        getMockExpiry(expiry = 900000)
        mockGenerateTokenRecordSetId()
        mockCreateClientAccessToken()
        mockCreateTokenRecordSet(success = true)

        awaitAndAssert(testOrchestrator.clientCredentialsGrant("testScope")) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "testScope",
            expiresIn = 900000,
            "testAccessToken",
            None
          )
        }
      }
    }

    "return InvalidClient" when {
      "the client could not be found" in {
        implicit val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic dGVzdElkOnRlc3RTZWNyZXQ=")

        mockGetRegisteredAppByIdAndSecret(app = None)

        awaitAndAssert(testOrchestrator.clientCredentialsGrant("testScope")) {
          _ mustBe InvalidClient
        }
      }

      "the basic auth header was invalid" in {
        implicit val req = FakeRequest()

        awaitAndAssert(testOrchestrator.clientCredentialsGrant("testScope")) {
          _ mustBe InvalidClient
        }
      }
    }
  }
}
