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

package controllers.actions

import helpers.Assertions
import helpers.services.MockClientService
import models.RegisteredApplication
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Ok
import play.api.mvc.{BaseController, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.oauth2.ClientService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BasicAuthActionSpec extends PlaySpec with MockClientService with Assertions {

  private val testFilter = new BasicAuthAction with BaseController {
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
    override val clientService: ClientService = mockClientService
    override implicit val ec: ExecutionContext = stubControllerComponents().executionContext
  }

  private val okFunction: RegisteredApplication => Future[Result] = app => Future.successful(Ok(s"I am app ${app.appId}"))

  val now: DateTime = DateTime.now()

  val testApp: RegisteredApplication = RegisteredApplication(
    appId        = "testAppId",
    owner        = "testOwner",
    name         = "testName",
    desc         = "testDesc",
    iconUrl      = None,
    homeUrl      = "http://localhost:8080",
    redirectUrl  = "http://localhost:8080/redirect",
    clientType   = "confidential",
    clientId     = "testId",
    clientSecret = Some("testSecret"),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq(),
    idTokenExpiry = 0L,
    accessTokenExpiry = 0L,
    refreshTokenExpiry = 0L,
    createdAt    = now
  )

  "clientAuthentication" should {
    "return an ok" when {
      "the client has been successfully validated" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic dGVzdElkOnRlc3RTZWNyZXQ=")

        mockGetRegisteredAppByIdAndSecret(app = Some(testApp))

        val result = testFilter.clientAuthentication {
          _ => app => _ => okFunction(app)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)          mustBe OK
          contentAsString(res) mustBe "I am app testAppId"
        }
      }
    }

    "return an unauthorized" when {
      "there was no auth header" in {
        val req = FakeRequest()

        val result = testFilter.clientAuthentication {
          _ => app => _ => okFunction(app)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res).\("error").as[String] mustBe "invalid_client"
        }
      }

      "the header was found but it wasn't a basic auth header" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Bearer 1234567890")

        val result = testFilter.clientAuthentication {
          _ => app => _ => okFunction(app)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res).\("error").as[String] mustBe "invalid_client"
        }
      }

      "the header was found, it was basic, but it could not be decoded" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic invalid-base64")

        val result = testFilter.clientAuthentication {
          _ => app => _ => okFunction(app)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res).\("error").as[String] mustBe "invalid_client"
        }
      }

      "the header was found, it was decoded, but no matching client could be found" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> "Basic dGVzdElkOnRlc3RTZWNyZXQ=")

        mockGetRegisteredAppByIdAndSecret(app = None)

        val result = testFilter.clientAuthentication {
          _ => app => _ => okFunction(app)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe UNAUTHORIZED
          contentAsJson(res).\("error").as[String] mustBe "invalid_client"
        }
      }
    }
  }
}
