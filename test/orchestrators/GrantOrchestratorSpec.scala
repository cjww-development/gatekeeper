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
import helpers.services.{MockAccountService, MockClientService, MockGrantService, MockScopeService}
import models._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.oauth2.{ClientService, GrantService, ScopeService}
import services.users.UserService

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class GrantOrchestratorSpec
  extends PlaySpec
    with Assertions
    with Obfuscators
    with MockGrantService
    with MockClientService
    with MockAccountService
    with MockScopeService {

  override val locale: String = ""

  val now: DateTime = new DateTime()

  val testOrchestrator: GrantOrchestrator = new GrantOrchestrator {
    override protected val clientService: ClientService = mockClientService
    override protected val scopeService: ScopeService = mockScopeService
    override protected val grantService: GrantService = mockGrantService
    override protected val userService: UserService = mockAccountService
  }

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
    oauth2Flows = Seq("authorization_code"),
    oauth2Scopes = Seq("username"),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = now
  )

  val testUser: User = User(
    id        = s"org-user-${UUID.randomUUID().toString}",
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
    authorisedClients = List(AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now)),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  "validateIncomingGrant" should {
    "return a ValidatedGrantRequest" when {
      "the app is found and redirects and scopes are valid and the app owner is found" in {
        mockGetRegisteredAppById(app = Some(testApp))
        mockValidateScopesWithApp(valid = true)
        mockGetValidScopes(scopes = Seq(Scope(
          name = "username",
          readableName = "username",
          desc = "testDesc"
        )))
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = "",
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

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, "username", testUser.id)) {
          _ mustBe ValidatedGrantRequest(testApp.copy(owner = "test-org"), Seq(Scope(
            name = "username",
            readableName = "username",
            desc = "testDesc"
          )))
        }
      }
    }

    "return a InvalidApplication" when {
      "the app wasn't found" in {
        mockGetRegisteredAppById(app = None)

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, "username", testUser.id)) {
          _ mustBe InvalidApplication
        }
      }
    }

    "return a InvalidResponseType" when {
      "the response type was not code" in {
        mockGetRegisteredAppById(app = None)

        awaitAndAssert(testOrchestrator.validateIncomingGrant("codez", testApp.clientId, "username", testUser.id)) {
          _ mustBe InvalidResponseType
        }
      }
    }

    "return a InvalidScopesRequested" when {
      "the scopes aren't valid" in {
        mockGetRegisteredAppById(app = Some(testApp))
        mockValidateScopes(valid = false)

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, "username", testUser.id)) {
          _ mustBe InvalidScopesRequested
        }
      }
    }
  }
}
