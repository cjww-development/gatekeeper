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

import helpers.Assertions
import helpers.services.{MockAccountService, MockGrantService, MockLoginService, MockScopeService}
import models.{AuthorisationRequest, Login, RegisteredApplication, Scopes, User}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.{AccountService, GrantService, LoginService, ScopeService}

import scala.concurrent.ExecutionContext.Implicits.global

class GrantOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockGrantService
    with MockAccountService
    with MockScopeService {

  val testOrchestrator: GrantOrchestrator = new GrantOrchestrator {
    override protected val scopeService: ScopeService = mockScopeService
    override protected val grantService: GrantService = mockGrantService
    override protected val accountService: AccountService = mockAccountService
  }

  val testApp: RegisteredApplication = RegisteredApplication(
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId",
    clientSecret = Some("testSecret")
  )

  "validateIncomingGrant" should {
    "return a ValidatedGrantRequest" when {
      "the app is found and redirects and scopes are valid and the app owner is found" in {
        mockGetRegisteredApp(app = Some(testApp))
        mockValidateRequestedScopes(valid = true)
        mockGetOrganisationAccountInfo(value = Map("userName" -> "test-org"))
        mockMakeScopesFromQuery(scopes = Scopes(Seq("read:username"), Seq()))

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, Seq("read:username"))) {
          _ mustBe ValidatedGrantRequest(testApp.copy(owner = "test-org"), Scopes(Seq("read:username"), Seq()))
        }
      }

      "the app is found and redirects and scopes are valid but the app owner isn't found" in {
        mockGetRegisteredApp(app = Some(testApp))
        mockValidateRequestedScopes(valid = true)
        mockGetOrganisationAccountInfo(value = Map())
        mockMakeScopesFromQuery(scopes = Scopes(Seq("read:username"), Seq()))

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, Seq("read:username"))) {
          _ mustBe ValidatedGrantRequest(testApp.copy(owner = ""), Scopes(Seq("read:username"), Seq()))
        }
      }
    }

    "return a InvalidApplication" when {
      "the app wasn't found" in {
        mockGetRegisteredApp(app = None)

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, Seq("read:username"))) {
          _ mustBe InvalidApplication
        }
      }
    }

    "return a InvalidScopesRequested" when {
      "the scopes aren't valid" in {
        mockGetRegisteredApp(app = Some(testApp))
        mockValidateRequestedScopes(valid = false)

        awaitAndAssert(testOrchestrator.validateIncomingGrant("code", testApp.clientId, Seq("read:username"))) {
          _ mustBe InvalidScopesRequested
        }
      }
    }
  }
}
