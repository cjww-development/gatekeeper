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

import dev.cjww.mongo.responses.{MongoFailedDelete, MongoFailedUpdate, MongoSuccessDelete, MongoSuccessUpdate}
import helpers.Assertions
import helpers.services.{MockAccountService, MockClientService, MockTokenService}
import models._
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.oauth2._
import services.users.UserService
import utils.StringUtils._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ClientOrchestratorSpec
  extends PlaySpec
    with Assertions
    with MockClientService
    with MockAccountService
    with MockTokenService {

  val testOrchestrator: ClientOrchestrator = new ClientOrchestrator {
    override protected val clientService: ClientService = mockClientService
    override protected val userService: UserService = mockAccountService
    override protected val tokenService: TokenService = mockTokenService
  }

  val now: DateTime = new DateTime()

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    iconUrl      = None,
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId".encrypt,
    clientSecret = Some("testSecret".encrypt),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = DateTime.now()
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
    createdAt = DateTime.now()
  )

  "getRegisteredApp" should {
    "return a registered app" when {
      "a valid app was found" in {
        mockGetRegisteredApp(app = Some(testApp))

        awaitAndAssert(testOrchestrator.getRegisteredApp("testOrgId", "testAppId")) {
          _ mustBe Some(testApp.copy(clientId = "testId", clientSecret = Some("testSecret")))
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockGetRegisteredApp(app = None)

        awaitAndAssert(testOrchestrator.getRegisteredApp("testOrgId", "testAppId")) {
          _ mustBe None
        }
      }
    }
  }

  "getRegisteredApps" should {
    "return some registered apps" when {
      "a valid apps were found" in {
        mockGetRegisteredApps(apps = Seq(testApp))

        awaitAndAssert(testOrchestrator.getRegisteredApps("testOrgId", 1)) {
          _ mustBe Seq(Seq(testApp.copy(clientId = "testId", clientSecret = Some("testSecret"))))
        }
      }
    }

    "return an empty Seq" when {
      "no apps were found" in {
        mockGetRegisteredApps(apps = Seq())

        awaitAndAssert(testOrchestrator.getRegisteredApps("testOrgId", 1)) {
          _ mustBe Seq()
        }
      }
    }
  }

  "regenerateClientIdAndSecret" should {
    "return a SecretsUpdated" when {
      "the client id and secret have been updated" in {
        mockGetRegisteredApp(app = Some(testApp))
        mockRegenerateClientIdAndSecret(resp = RegeneratedIdAndSecret)

        awaitAndAssert(testOrchestrator.regenerateClientIdAndSecret(testApp.owner, testApp.appId)) {
          _ mustBe SecretsUpdated
        }
      }

      "the client id have been updated" in {
        mockGetRegisteredApp(app = Some(testApp.copy(clientType = "public", clientSecret = None)))
        mockRegenerateClientIdAndSecret(resp = RegeneratedId)

        awaitAndAssert(testOrchestrator.regenerateClientIdAndSecret(testApp.owner, testApp.appId)) {
          _ mustBe SecretsUpdated
        }
      }
    }

    "return a UpdateFailed" when {
      "there was an issue regenerating the secrets" in {
        mockGetRegisteredApp(app = Some(testApp))
        mockRegenerateClientIdAndSecret(resp = RegenerationFailed)

        awaitAndAssert(testOrchestrator.regenerateClientIdAndSecret(testApp.owner, testApp.appId)) {
          _ mustBe UpdatedFailed
        }
      }
    }

    "return a NoAppFound" when {
      "the app could not be found first" in {
        mockGetRegisteredApp(app = None)

        awaitAndAssert(testOrchestrator.regenerateClientIdAndSecret(testApp.owner, testApp.appId)) {
          _ mustBe NoAppFound
        }
      }
    }
  }

  "deleteClient" should {
    "return MongoSuccessDelete" when {
      "the app was deleted" in {
        mockDeleteClient(resp = MongoSuccessDelete)

        awaitAndAssert(testOrchestrator.deleteClient("testOrgId", "testAppId")) {
          _ mustBe MongoSuccessDelete
        }
      }
    }

    "return MongoFailedDelete" when {
      "the app was deleted" in {
        mockDeleteClient(resp = MongoFailedDelete)

        awaitAndAssert(testOrchestrator.deleteClient("testOrgId", "testAppId")) {
          _ mustBe MongoFailedDelete
        }
      }
    }
  }

  "getAuthorisedApps" should {
    "return a list of apps" when {
      "the user has authorised apps against their account" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testUser.id,
          userName = testUser.userName,
          email = testUser.digitalContact.email.address,
          emailVerified = testUser.digitalContact.email.verified,
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
          accType = testUser.accType,
          authorisedClients = List(AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now)),
          mfaEnabled = false,
          createdAt = DateTime.now()
        )))

        mockGetRegisteredAppByAppId(app = Some(testApp))


        awaitAndAssert(testOrchestrator.getAuthorisedApps(testUser.id)) {
          _ mustBe List(testApp)
        }
      }
    }

    "return an empty list of apps" when {
      "the user has no authorised apps against their account" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testUser.id,
          userName = testUser.userName,
          email = testUser.digitalContact.email.address,
          emailVerified = testUser.digitalContact.email.verified,
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
          accType = testUser.accType,
          authorisedClients = List(),
          mfaEnabled = false,
          createdAt = DateTime.now()
        )))

        awaitAndAssert(testOrchestrator.getAuthorisedApps(testUser.id)) {
          _ mustBe List()
        }
      }

      "the user does not exist" in {
        mockGetOrganisationAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getAuthorisedApps(testUser.id)) {
          _ mustBe List()
        }
      }
    }
  }

  "getAuthorisedApp" should {
    "return a RegisteredApplication" when {
      "the user is valid, the app has been found and the org data is found" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testUser.id,
          userName = testUser.userName,
          email = testUser.digitalContact.email.address,
          emailVerified = testUser.digitalContact.email.verified,
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
          accType = testUser.accType,
          authorisedClients = List(AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now)),
          mfaEnabled = false,
          createdAt = DateTime.now()
        )))

        mockGetRegisteredAppByAppId(app = Some(testApp))
        mockGetActiveSessionsFor(sessions = Seq())

        awaitAndAssert(testOrchestrator.getAuthorisedApp(testUser.id, testApp.appId)) {
          _ mustBe Some((testApp.copy(owner = testUser.userName)), AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now), Seq())
        }
      }
    }

    "return None" when {
      "the user cannot be found" in {
        mockGetOrganisationAccountInfo(value = None)

        awaitAndAssert(testOrchestrator.getAuthorisedApp(testUser.id, testApp.appId)) {
          _ mustBe None
        }
      }

      "the user has not previously authorised the app" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testUser.id,
          userName = testUser.userName,
          email = testUser.digitalContact.email.address,
          emailVerified = testUser.digitalContact.email.verified,
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
          accType = testUser.accType,
          authorisedClients = List(AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now)),
          mfaEnabled = false,
          createdAt = DateTime.now()
        )))

        mockGetRegisteredAppByAppId(app = None)

        awaitAndAssert(testOrchestrator.getAuthorisedApp(testUser.id, testApp.appId)) {
          _ mustBe None
        }
      }

      "the app cannot be found" in {
        mockGetOrganisationAccountInfo(value = Some(UserInfo(
          id = testUser.id,
          userName = testUser.userName,
          email = testUser.digitalContact.email.address,
          emailVerified = testUser.digitalContact.email.verified,
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
          accType = testUser.accType,
          authorisedClients = List(AuthorisedClient(appId = testApp.appId, authorisedScopes = Seq(), authorisedOn = now)),
          mfaEnabled = false,
          createdAt = DateTime.now()
        )))

        mockGetRegisteredAppByAppId(app = None)

        awaitAndAssert(testOrchestrator.getAuthorisedApp(testUser.id, testApp.appId)) {
          _ mustBe None
        }
      }
    }
  }



  "updateBasicDetails" should {
    "return a MongoSuccessUpdate" when {
      "the apps basic details were updated" in {
        mockUpdateBasicDetails(resp = MongoSuccessUpdate)

        awaitAndAssert(testOrchestrator.updateBasicDetails("testOwner", "testAppId", "testName", "testDesc", Some("testIconUrl"))) {
          _ mustBe BasicsUpdates
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the apps basic details could not be updated" in {
        mockUpdateBasicDetails(resp = MongoFailedUpdate)

        awaitAndAssert(testOrchestrator.updateBasicDetails("testOwner", "testAppId", "testName", "testDesc", Some("testIconUrl"))) {
          _ mustBe UpdatedFailed
        }
      }
    }
  }
}
