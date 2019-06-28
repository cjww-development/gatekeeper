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

import com.cjwwdev.http.headers.HeaderPackage
import com.cjwwdev.implicits.ImplicitDataSecurity._
import com.cjwwdev.implicits.ImplicitJsValues._
import com.cjwwdev.security.obfuscation.Obfuscation._
import com.cjwwdev.testing.integration.IntegrationTestSpec
import com.cjwwdev.testing.integration.application.IntegrationApplication
import com.typesafe.config.ConfigFactory
import database.RegisteredApplicationsStore
import models.{ClientTypes, RegisteredApplication}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import play.api.{Application, Configuration}
import slick.jdbc.MySQLProfile.api._

class ApplicationsAPIISpec extends IntegrationTestSpec with IntegrationApplication {

  val testRepo = app.injector.instanceOf[RegisteredApplicationsStore]

  override val currentAppBaseUrl: String   = "gatekeeper"
  override val appConfig: Map[String, Any] = Map("" -> "")

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Configuration(ConfigFactory.load()))
    .build()

  override def beforeEach(): Unit = {
    await(testRepo.getDb.run(testRepo.table.schema.create))
  }

  override def afterEach(): Unit = {
    await(testRepo.getDb.run(testRepo.table.schema.drop))
  }

  def client(url: String): WSRequest = {
    ws.url(s"$testAppUrl$url")
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
  ).encrypt

  val headers = List(
    CONTENT_TYPE   -> TEXT,
    "cjww-headers" -> HeaderPackage("d6e3a79b-cb31-40a1-839a-530803d76156", None).encrypt,
    "requestId"    -> s"${UUID.randomUUID()}"
  )

  "/gatekeeper/register-client" should {
    "return an Ok" when {
      "the app has been successfully registered" in {
        awaitAndAssert(client("/register-client").withHttpHeaders(headers:_*).post(jsonBody)) { resp =>
          resp.status                   mustBe CREATED
          resp.json.get[String]("body") mustBe "Registered new application testName"
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
        ).encrypt

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
          resp.status                           mustBe BAD_REQUEST
          resp.json.get[String]("errorMessage") mustBe "There was a problem creating the application testName"
        }
      }
    }
  }

  "/gatekeeper/clients" should {
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

        implicit val reads = Json.reads[RegisteredApplication]

        awaitAndAssert(client("/clients").withHttpHeaders(headers:_*).get()) { resp =>
          resp.status                                       mustBe OK
          resp.json.get[Seq[RegisteredApplication]]("body") mustBe Seq(RegisteredApplication(
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

  "/gatekeeper/client/testName" should {
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
          resp.status                           mustBe BAD_REQUEST
          resp.json.get[String]("errorMessage") mustBe "There was a problem removing the application testName"
        }
      }
    }
  }
}
