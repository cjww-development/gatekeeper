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

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import database.AppStore
import helpers.Assertions
import helpers.database.MockAppStore
import models.RegisteredApplication
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.deobfuscation.DeObfuscators
import com.cjwwdev.security.obfuscation.Obfuscators

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
  }

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "test-app-name",
    desc         = "test desc",
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/rediect",
    clientType   = "confidential",
    clientId     = "testClientId".encrypt,
    clientSecret = Some("testClientSecret".encrypt),
    createdAt    = DateTime.now()
  )

  "getRegisteredApp" should {
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
}
