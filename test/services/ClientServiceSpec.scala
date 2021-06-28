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

import database.AppStore
import dev.cjww.mongo.responses.{MongoFailedDelete, MongoFailedUpdate, MongoSuccessDelete, MongoSuccessUpdate}
import dev.cjww.security.Implicits._
import dev.cjww.security.deobfuscation.DeObfuscators
import dev.cjww.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.database.MockAppStore
import models.{PresetService, RegisteredApplication}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import services.oauth2.{ClientService, RegeneratedId, RegeneratedIdAndSecret, RegenerationFailed}

import scala.concurrent.ExecutionContext.Implicits.global

class ClientServiceSpec
  extends PlaySpec
    with Assertions
    with Obfuscators
    with DeObfuscators
    with MockAppStore {

  override val locale: String = ""

  private val testService: ClientService = new ClientService {
    override val appStore: AppStore = mockAppStore
    override protected val presetServices: Seq[PresetService] = Seq(
      PresetService(
        name = "test service",
        desc = "For test integrations",
        icon = "/images/icon.png",
        domain = Some("https://test.example.com"),
        redirect = "/callback"
      ),
      PresetService(
        name = "test service",
        desc = "For test integrations",
        icon = "/images/icon.png",
        domain = None,
        redirect = "/callback"
      )
    )
  }

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "test-app-name",
    desc         = "test desc",
    iconUrl      = None,
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/rediect",
    clientType   = "confidential",
    clientId     = "testClientId".encrypt,
    clientSecret = Some("testClientSecret".encrypt),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = DateTime.now()
  )

  "getPresetServices" should {
    "return a sequence of preset services" in {
      assertOutput(testService.getPresetServices) { services =>
        services.length mustBe 2

        services.head.name mustBe "test service"
        services.head.desc mustBe "For test integrations"
        services.head.icon mustBe "/images/icon.png"
        services.head.domain mustBe Some("https://test.example.com")
        services.head.redirect mustBe "/callback"

        services.last.name mustBe "test service"
        services.last.desc mustBe "For test integrations"
        services.last.icon mustBe "/images/icon.png"
        services.last.domain mustBe None
        services.last.redirect mustBe "/callback"
      }
    }
  }

  "getRegisteredApp (orgUserId and appId)" should {
    "return a registered app" when {
      "a valid app was found" in {
        mockValidateAppOn(app = Some(testApp))

        awaitAndAssert(testService.getRegisteredApp("testOrgId", "testAppId")) {
          _ mustBe Some(testApp)
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockValidateAppOn(app = None)

        awaitAndAssert(testService.getRegisteredApp("testOrgId", "testAppId")) {
          _ mustBe None
        }
      }
    }
  }

  "getRegisteredApp (appId)" should {
    "return a registered app" when {
      "a valid app was found" in {
        mockValidateAppOn(app = Some(testApp))

        awaitAndAssert(testService.getRegisteredApp("testAppId")) {
          _ mustBe Some(testApp)
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockValidateAppOn(app = None)

        awaitAndAssert(testService.getRegisteredApp("testAppId")) {
          _ mustBe None
        }
      }
    }
  }

  "getRegisteredAppByIdAndSecret" should {
    "return a registered app" when {
      "a valid app was found against the client id and secret" in {
        mockValidateAppOn(app = Some(testApp))

        awaitAndAssert(testService.getRegisteredAppByIdAndSecret("testClientId", "testClientSecret")) {
          _ mustBe Some(testApp)
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockValidateAppOn(app = None)

        awaitAndAssert(testService.getRegisteredAppByIdAndSecret("testClientId", "testClientSecret")) {
          _ mustBe None
        }
      }
    }
  }

  "getRegisteredAppById" should {
    "return a registered app" when {
      "a valid app was found against the client id" in {
        mockValidateAppOn(app = Some(testApp))

        awaitAndAssert(testService.getRegisteredAppById("testClientId")) {
          _ mustBe Some(testApp)
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockValidateAppOn(app = None)

        awaitAndAssert(testService.getRegisteredAppById("testClientId")) {
          _ mustBe None
        }
      }
    }
  }

  "getRegisteredAppsFor" should {
    "return a registered app" when {
      "a valid app was found" in {
        mockGetAppsOwnedBy(apps = Seq(testApp))

        awaitAndAssert(testService.getRegisteredAppsFor("testOrgId")) {
          _ mustBe Seq(testApp)
        }
      }
    }

    "return None" when {
      "no app was found" in {
        mockGetAppsOwnedBy(apps = Seq())

        awaitAndAssert(testService.getRegisteredAppsFor("testOrgId")) {
          _ mustBe Seq()
        }
      }
    }
  }

  "regenerateClientIdAndSecret" should {
    "return a RegeneratedIdAndSecret" when {
      "the ids and secret were updated and the app was confidential" in {
        mockUpdateApp(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.regenerateClientIdAndSecret("testOrgId", "testAppId", isConfidential = true)) {
          _ mustBe RegeneratedIdAndSecret
        }
      }
    }

    "return a RegeneratedId" when {
      "the id was updated and the app was individual" in {
        mockUpdateApp(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.regenerateClientIdAndSecret("testOrgId", "testAppId", isConfidential = false)) {
          _ mustBe RegeneratedId
        }
      }
    }

    "return a RegeneratedFailed" when {
      "there was a problem updating the app" in {
        mockUpdateApp(resp = MongoFailedUpdate)

        awaitAndAssert(testService.regenerateClientIdAndSecret("testOrgId", "testAppId", isConfidential = false)) {
          _ mustBe RegenerationFailed
        }
      }
    }
  }

  "deleteClient" should {
    "return a MongoSuccessDelete" when {
      "the app was deleted" in {
        mockDeleteApp(resp = MongoSuccessDelete)

        awaitAndAssert(testService.deleteClient("testOrgId", "testAppId")) {
          _ mustBe MongoSuccessDelete
        }
      }
    }

    "return a MongoFailedDelete" when {
      "the app was deleted" in {
        mockDeleteApp(resp = MongoFailedDelete)

        awaitAndAssert(testService.deleteClient("testOrgId", "testAppId")) {
          _ mustBe MongoFailedDelete
        }
      }
    }
  }

  "updateBasicDetails" should {
    "return a MongoSuccessUpdate" when {
      "the apps basic details were updated" in {
        mockUpdateApp(resp = MongoSuccessUpdate)

        awaitAndAssert(testService.updateBasicDetails("testOwner", "testAppId", "testName", "testDesc", Some("testIconUrl"))) {
          _ mustBe MongoSuccessUpdate
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the apps basic details could not be updated" in {
        mockUpdateApp(resp = MongoFailedUpdate)

        awaitAndAssert(testService.updateBasicDetails("testOwner", "testAppId", "testName", "testDesc", Some("testIconUrl"))) {
          _ mustBe MongoFailedUpdate
        }
      }
    }
  }
}
