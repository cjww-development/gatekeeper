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

import dev.cjww.security.Implicits._
import dev.cjww.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.services._
import models._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.oauth2.{ClientService, GrantService, ScopeService, TokenService}
import services.users.UserService

import java.util.UUID
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
    oauth2Flows = Seq("authorization_code", "client_credentials", "refresh_token"),
    oauth2Scopes = Seq("openid"),
    idTokenExpiry = 900000L,
    accessTokenExpiry = 900000L,
    refreshTokenExpiry = 900000L,
    createdAt    = DateTime.now()
  )

  val testIndUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUsername",
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
    createdAt = now
  )

  val testOrgUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUsername",
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
          phone = None,
          phoneVerified = false,
          accType = "",
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
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
        mockCreateRefreshToken()
        mockCreateTokenRecordSet(success = true)
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", testApp, "testRedirect", None)) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "openid",
            expiresIn = 900000,
            "testAccessToken",
            Some("testIdToken"),
            Some("testRefreshToken")
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
          phone = None,
          phoneVerified = false,
          accType = "",
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
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
        mockCreateRefreshToken()
        mockCreateTokenRecordSet(success = true)
        getMockExpiry(expiry = 900000)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", testApp, "testRedirect", None)) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "openid",
            expiresIn = 900000,
            "testAccessToken",
            Some("testIdToken"),
            Some("testRefreshToken")
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

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", testApp, "testRedirect", None)) {
          _ mustBe InvalidUser
        }
      }
    }

    "return an invalid grant" when {
      "the grant could not be validated" in {
        mockValidateGrant(grant = None)

        awaitAndAssert(testOrchestrator.authorizationCodeGrant("testAuthCode", testApp, "testRedirect", None)) {
          _ mustBe InvalidGrant
        }
      }
    }
  }

  "clientCredentialsGrant" should {
    "return issued" when {
      "the client was validated" in {
        getMockExpiry(expiry = 900000)
        mockGenerateTokenRecordSetId()
        mockCreateClientAccessToken()
        mockCreateTokenRecordSet(success = true)

        awaitAndAssert(testOrchestrator.clientCredentialsGrant(testApp, "testScope")) {
          _ mustBe Issued(
            tokenType = "Bearer",
            scope = "testScope",
            expiresIn = 900000,
            "testAccessToken",
            None,
            None
          )
        }
      }
    }

    "return InvalidOAuthFlow" when {
      "the client could not be found" in {
        awaitAndAssert(testOrchestrator.clientCredentialsGrant(testApp.copy(oauth2Flows = Seq()), "testScope")) {
          _ mustBe InvalidOAuthFlow
        }
      }
    }
  }

  "refreshTokenGrant" should {
    val refreshToken = RefreshToken.enc(RefreshToken(
      sub = "testUserId",
      aud = "testClientId",
      iss = "testIssuer",
      iat = 100L,
      exp = 900L,
      tsid = "testTokenSetId",
      tid = "testTokenId",
      scope = Seq("testScope")
    ))

    "return Issued" when {
      "new id and access tokens have been issued" in {
        mockGetScopeDetails(scopes = Seq(Scope(name = "testScope", readableName = "testScope", desc = "testScope")))
        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = "testUserId",
          userName = "test-org",
          email = "",
          emailVerified = false,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = "",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))
        mockGetRegisteredAppById(app = Some(testApp))
        mockUpdateTokenRecordSet(success = true)

        awaitAndAssert(testOrchestrator.refreshTokenGrant(testApp, refreshToken)) {
          case Issued(tokenType, scope, expiresIn, accessToken, idToken, refreshToken) =>
            tokenType mustBe "Bearer"
            scope mustBe "testScope"
          case e =>
            fail("TokenResponse was not of type Issued")
        }
      }
    }

    "return a TokenError" when {
      "the refresh token could not be decrypted" in {
        awaitAndAssert(testOrchestrator.refreshTokenGrant(testApp, "invalid-token")) {
          _ mustBe TokenError
        }
      }

      "there was an issue updating the token record set" in {
        mockGetScopeDetails(scopes = Seq(Scope(name = "testScope", readableName = "testScope", desc = "testScope")))
        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = "testUserId",
          userName = "test-org",
          email = "",
          emailVerified = false,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = "",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))
        mockGetRegisteredAppById(app = Some(testApp))
        mockUpdateTokenRecordSet(success = false)

        awaitAndAssert(testOrchestrator.refreshTokenGrant(testApp, refreshToken)) {
          _ mustBe TokenError
        }
      }
    }

    "return an InvalidOAuthFlow" when {
      "the requesting client isn't allowed to use the refresh token flow" in {
        mockGetScopeDetails(scopes = Seq(Scope(name = "testScope", readableName = "testScope", desc = "testScope")))
        mockGetIndividualAccountInfo(value = Some(UserInfo(
          id = "testUserId",
          userName = "test-org",
          email = "",
          emailVerified = false,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = "",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        awaitAndAssert(testOrchestrator.refreshTokenGrant(testApp.copy(oauth2Flows = Seq()), refreshToken)) {
          _ mustBe InvalidOAuthFlow
        }
      }
    }

    "return an InvalidUser" when {
      "the user doesn't exist" in {
        mockGetScopeDetails(scopes = Seq(Scope(name = "testScope", readableName = "testScope", desc = "testScope")))
        mockGetIndividualAccountInfo(value = None)


        awaitAndAssert(testOrchestrator.refreshTokenGrant(testApp, refreshToken)) {
          _ mustBe InvalidUser
        }
      }
    }
  }
}
