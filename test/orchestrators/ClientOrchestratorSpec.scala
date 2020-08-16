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

import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.services.MockClientService
import models.RegisteredApplication
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.ClientService

import scala.concurrent.ExecutionContext.Implicits.global

class ClientOrchestratorSpec
  extends PlaySpec
    with Assertions
    with Obfuscators
    with MockClientService {

  override val locale: String = ""

  val testOrchestrator: ClientOrchestrator = new ClientOrchestrator {
    override val locale: String = ""
    override protected val clientService: ClientService = mockClientService
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
    createdAt    = DateTime.now()
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
}
