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

import database.{AppStore, GrantStore}
import dev.cjww.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import dev.cjww.security.Implicits._
import dev.cjww.security.obfuscation.Obfuscators
import helpers.Assertions
import helpers.database.{MockAppStore, MockGrantStore}
import models.{Grant, RegisteredApplication}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import services.oauth2.GrantService

import scala.concurrent.ExecutionContext.Implicits.global

class GrantServiceSpec
  extends PlaySpec
    with Assertions
    with Obfuscators
    with MockGrantStore
    with MockAppStore {

  override val locale: String = ""

  private val testService: GrantService = new GrantService {
    override val appStore: AppStore = mockAppStore
    override val grantStore: GrantStore = mockGrantStore
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
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = DateTime.now()
  )

  val testGrant: Grant = Grant(
    responseType = "code",
    authCode = "testAuthCode",
    scope = Seq("testScope"),
    clientId = testApp.clientId,
    userId = "testUserId",
    accType = "testType",
    redirectUri = testApp.redirectUrl,
    codeVerifier = None,
    codeChallenge = None,
    codeChallengeMethod = None,
    createdAt = DateTime.now()
  )

  "validateRedirectUrl" should {
    "return true" when {
      "the two redirects match" in {
        assertOutput(testService.validateRedirectUrl("http://localhost:8080/redirect", testApp.redirectUrl)) {
          res => assert(res)
        }
      }
    }

    "return false" when {
      "the two redirects don't match" in {
        assertOutput(testService.validateRedirectUrl("http://localhost:8080/redirect", "http://localhost:8080/redirect/abc")) {
          res => assert(!res)
        }
      }
    }
  }

  "saveGrant" should {
    "return a MongoCreateResponse" when {
      "successfully saving a grant" in {
        mockCreateGrant(success = true)

        awaitAndAssert(testService.saveGrant(testGrant)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoFailedCreate" when {
      "unsuccessfully saving a grant" in {
        mockCreateGrant(success = false)

        awaitAndAssert(testService.saveGrant(testGrant)) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "validateGrant" should {
    "return a grant" when {
      "the auth code and state have been validated" in {
        mockValidateGrant(app = Some(testGrant))

        awaitAndAssert(testService.validateGrant(testGrant.authCode, testApp.clientId, testApp.redirectUrl, None)) {
          _ mustBe Some(testGrant)
        }
      }
    }

    "return None" when {
      "the auth code and state could not be validated" in {
        mockValidateGrant(app = None)

        awaitAndAssert(testService.validateGrant(testGrant.authCode, testApp.clientId, testApp.redirectUrl, None)) {
          _ mustBe None
        }
      }
    }
  }
}
