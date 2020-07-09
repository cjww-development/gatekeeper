/*
 * Copyright 2019 CJWW Development
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

package api

import java.util.UUID

import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.obfuscation.Obfuscators
import com.typesafe.config.ConfigFactory
import database.RegisteredApplicationsStore
import models.RegisteredApplication._
import models.{ClientTypes, RegisteredApplication}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.ContentTypes._
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.{Application, Configuration}
import utils.IntegrationSpec

class ApplicationsAPIISpec extends IntegrationSpec with Obfuscators with GuiceOneServerPerSuite {

  override val locale: String = "ApplicationsAPIISpec"

  val testRepo = app.injector.instanceOf[RegisteredApplicationsStore]
  val wsClient = app.injector.instanceOf[WSClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Configuration(ConfigFactory.load()))
    .build()

  override def beforeEach(): Unit = {

  }

  override def afterEach(): Unit = {

  }

  def client(url: String): WSRequest = {
    wsClient.url(s"http://localhost:${port}/gatekeeper/$url")
  }

  System.setProperty("features.app-registration-api", "true")

  val jsonBody = Json.parse(
    """
      |{
      |   "name" : "testName",
      |   "desc" : "testDesc",
      |   "homeUrl" : "/test/url",
      |   "redirectUrl" : "/test/url",
      |   "clientType" : "confidential"
      |}
    """.stripMargin
  )

  val headers = List(
    CONTENT_TYPE -> TEXT,
    "requestId"  -> s"${UUID.randomUUID()}"
  )

  "POST /gatekeeper/register-client" should {
    "return an Ok" when {
      "the app has been successfully registered" in {
        awaitAndAssert(client("/register-client").withHttpHeaders(headers:_*).post(jsonBody)) { resp =>
          resp.status                    mustBe CREATED
          resp.json.\("body").as[String] mustBe "Registered new application testName"
        }
      }
    }

    "return a BadRequest" when {
      "the json body is malformed" in {
        val jsonBody = Json.parse(
          """
            |{
            |   "desc" : "testDesc",
            |   "homeUrl" : "/test/url",
            |   "redirectUrl" : "/test/url",
            |   "clientType" : "confidential"
            |}
          """.stripMargin
        )

        awaitAndAssert(client("/register-client").withHttpHeaders(headers:_*).post(jsonBody)) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }

      "there's an attempt to registered a pre-existing application" in {

        await(testRepo.insertNewApplication(RegisteredApplication(
          name         = "testName",
          desc         = "testDesc",
          homeUrl      = "/test/url",
          redirectUrl  = "/test/url",
          clientType   = ClientTypes.confidential.toString,
          clientId     = s"${UUID.randomUUID()}".replace("-", "").encrypt,
          clientSecret = Some(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").encrypt)
        )))

        awaitAndAssert(client("/register-client").withHttpHeaders(headers:_*).post(jsonBody)) { resp =>
          resp.status                            mustBe BAD_REQUEST
          resp.json.\("errorMessage").as[String] mustBe "There was a problem creating the application testName"
        }
      }
    }
  }

  "GET /gatekeeper/clients" should {
    "return an Ok" when {
      "there are registered applications" in {
        val clientId = s"${UUID.randomUUID()}".replace("-", "").encrypt
        val clientSecret = Some(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").encrypt)

        await(testRepo.insertNewApplication(RegisteredApplication(
          name         = "testName",
          desc         = "testDesc",
          homeUrl      = "/test/url",
          redirectUrl  = "/test/url",
          clientType   = ClientTypes.confidential.toString,
          clientId,
          clientSecret
        )))

        val reads = Reads.seq(Json.reads[RegisteredApplication])

        awaitAndAssert(client("/clients").withHttpHeaders(headers:_*).get()) { resp =>
          resp.status                                        mustBe OK
          resp.json.\("body").as[Seq[RegisteredApplication]](reads) mustBe Seq(RegisteredApplication(
            "testName",
            "testDesc",
            "/test/url",
            "/test/url",
            "confidential",
            clientId,
            clientSecret
          ))
        }
      }
    }

    "return a NoContent" when {
      "there are no registered applications" in {
        awaitAndAssert(client("/clients").withHttpHeaders(headers:_*).get()) {
          _.status mustBe NO_CONTENT
        }
      }
    }
  }

  "GET /gatekeeper/client/testName" should {
    "return an Ok" when {
      "a service could be found based on the key" in {
        val clientId = s"${UUID.randomUUID()}".replace("-", "").encrypt
        val clientSecret = Some(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").encrypt)

        await(testRepo.insertNewApplication(RegisteredApplication(
          name         = "testName",
          desc         = "testDesc",
          homeUrl      = "/test/url",
          redirectUrl  = "/test/url",
          clientType   = ClientTypes.confidential.toString,
          clientId,
          clientSecret
        )))

        implicit val reads = Json.reads[RegisteredApplication]

        awaitAndAssert(client("/client/testName").withHttpHeaders(headers:_*).get()) { resp =>
          resp.status                                   mustBe OK
          resp.json.\("body").as[RegisteredApplication](reads) mustBe RegisteredApplication(
            "testName",
            "testDesc",
            "/test/url",
            "/test/url",
            "confidential",
            clientId,
            clientSecret
          )
        }
      }
    }

    "return a NotFound" when {
      "a service could not be found based on the key" in {
        awaitAndAssert(client("/client/testName").withHttpHeaders(headers:_*).get()) {
          _.status mustBe NOT_FOUND
        }
      }
    }
  }


  "DELETE /gatekeeper/client/testName" should {
    "return a NoContent" when {
      "the specified application was be deleted" in {
        val clientId = s"${UUID.randomUUID()}".replace("-", "").encrypt
        val clientSecret = Some(s"${UUID.randomUUID()}${UUID.randomUUID()}".replace("-", "").encrypt)

        await(testRepo.insertNewApplication(RegisteredApplication(
          name         = "testName",
          desc         = "testDesc",
          homeUrl      = "/test/url",
          redirectUrl  = "/test/url",
          clientType   = ClientTypes.confidential.toString,
          clientId,
          clientSecret
        )))

        awaitAndAssert(client("/client/testName").withHttpHeaders(headers:_*).delete()) {
          _.status mustBe NO_CONTENT
        }
      }
    }

    "return a BadRequest" when {
      "the specified application couldn't be deleted" in {
        awaitAndAssert(client("/client/testName").withHttpHeaders(headers:_*).delete()) { resp =>
          resp.status                            mustBe BAD_REQUEST
          resp.json.\("errorMessage").as[String] mustBe "There was a problem removing the application testName"
        }
      }
    }
  }
}
