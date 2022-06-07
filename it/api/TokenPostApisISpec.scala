/*
 * Copyright 2021 CJWW Development
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

import database._
import helpers.{Assertions, IntegrationApp, PlaySession}
import models._
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSAuthScheme, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._
import services.oauth2.{ClientService, TokenService}
import utils.StringUtils
import utils.StringUtils._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TokenPostApisISpec
  extends PlaySpec
    with IntegrationApp
    with PlaySession
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val orgUserStore: UserStore = app.injector.instanceOf[OrganisationUserStore]
  lazy val appStore: AppStore = app.injector.instanceOf[AppStore]
  lazy val clientService: ClientService = app.injector.instanceOf[ClientService]

  val uuid: String = UUID.randomUUID().toString
  val now: DateTime = DateTime.now()
  val saltString: String = StringUtils.salter(length = 32)

  val testOrganisationUser: User = User(
    userName = "testOrgUserName",
    email = "test-org@email.com",
    accType = "organisation",
    password = "testPassword"
  )

  val testClient: RegisteredApplication = RegisteredApplication(
    owner = testOrganisationUser.id,
    name = "Test client",
    desc = "Test description",
    homeUrl = "testHomeUrl",
    redirectUrl = "testRedirectUrl",
    clientType = "confidential",
    iconUrl = None,
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(orgUserStore.createUser(testOrganisationUser))
    await(appStore.createApp(testClient))
    await(clientService.updateOAuth2Flows(Seq("client_credentials"), testClient.appId, testOrganisationUser.id))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
  }

  "POST /oauth2/token" should {
    "return an Ok" when {
      "a client credentials token has been issued" in {
        val clientId = testClient.clientId.decrypt.getOrElse("")
        val clientSecret = testClient.clientSecret.get.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/token")
          .withFollowRedirects(follow = false)
          .withAuth(clientId, clientSecret, WSAuthScheme.BASIC)
          .post(Map(
            "grant_type" -> Seq("client_credentials"),
            "scope" -> Seq("test"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
        }
      }
    }
  }
}
