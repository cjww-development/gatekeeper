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

package ui

import database._
import helpers.{Assertions, IntegrationApp}
import models.{AuthorisedClient, RegisteredApplication, ServerCookies, User}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._
import utils.StringUtils._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2GetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val individualUserStore: UserStore = app.injector.instanceOf[IndividualUserStore]
  lazy val orgUserStore: UserStore = app.injector.instanceOf[OrganisationUserStore]
  lazy val appStore: AppStore = app.injector.instanceOf[AppStore]

  val uuid: String = UUID.randomUUID().toString
  val now: DateTime = DateTime.now()

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  val testIndividualUserTwo: User = User(
    userName = "testTwoUserName",
    email = "test-two@email.com",
    accType = "individual",
    password = "testPassword"
  ).copy(authorisedClients = List(
    AuthorisedClient(
      appId = s"appId-${uuid}",
      authorisedScopes = List("openid", "email"),
      authorisedOn = now
    )
  ))

  val testOrganisationUser: User = User(
    userName = "testOrgUserName",
    email = "test-org@email.com",
    accType = "organisation",
    password = "testPassword"
  )

  val testClient: RegisteredApplication = RegisteredApplication(
    appId = s"appId-$uuid",
    owner = testOrganisationUser.id,
    name = "test-app",
    desc = "test desc",
    iconUrl = None,
    homeUrl = "http://localhost:1234/home",
    redirectUrl = "http://localhost:1234/redirect",
    clientType = "confidential",
    clientId = RegisteredApplication.generateIds(iterations = 0),
    clientSecret = Some(RegisteredApplication.generateIds(iterations = 1)),
    oauth2Flows = Seq("authorization_code", "client_credentials"),
    oauth2Scopes = Seq("openid", "profile", "email", "phone", "address"),
    idTokenExpiry = 1,
    accessTokenExpiry = 1,
    refreshTokenExpiry = 1,
    createdAt = now
  )

  val testClientTwo: RegisteredApplication = RegisteredApplication(
    appId = s"appId-${UUID.randomUUID().toString}",
    owner = testOrganisationUser.id,
    name = "test-app",
    desc = "test desc",
    iconUrl = None,
    homeUrl = "http://localhost:1234/home",
    redirectUrl = "http://localhost:1234/redirect",
    clientType = "confidential",
    clientId = RegisteredApplication.generateIds(iterations = 0),
    clientSecret = Some(RegisteredApplication.generateIds(iterations = 1)),
    oauth2Flows = Seq(),
    oauth2Scopes = Seq("openid", "profile", "email", "phone", "address"),
    idTokenExpiry = 1,
    accessTokenExpiry = 1,
    refreshTokenExpiry = 1,
    createdAt = now
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    await(individualUserStore.createUser(testIndividualUser))
    await(individualUserStore.createUser(testIndividualUserTwo))
    await(orgUserStore.createUser(testOrganisationUser))
    await(appStore.createApp(testClient))
    await(appStore.createApp(testClientTwo))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUserTwo.id)).toFuture())
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClientTwo.appId)).toFuture())
  }

  "GET /oauth2/authorize" should {
    "return an Ok" when {
      "the user is presented with the OAuth2 consent screen" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=$clientId&scope=openid&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Allow access"
        }
      }

      "the user is presented with the OAuth2 consent screen because the app has changed what scopes it requires" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUserTwo.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=$clientId&scope=openid&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Allow access"
        }
      }
    }

    "return a See other" when {
      "the requesting app has been previously authorised by the user and an auth code has been returned" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUserTwo.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=$clientId&scope=openid%20email&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains(testClient.redirectUrl))
        }
      }
    }

    "return a Bad request" when {
      "the requesting client is trying to access scopes that aren't valid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=$clientId&scope=open&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          resp.body mustBe "InvalidScopesRequested"
        }
      }

      "the requesting client is trying to access flows that aren't valid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClientTwo.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=$clientId&scope=open&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          resp.body mustBe "InvalidOAuth2Flow"
        }
      }

      "the requesting client doesn't exist" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=code&client_id=invalid-id&scope=open&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          resp.body mustBe "InvalidApplication"
        }
      }

      "the requesting client requesting an invalid response type" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClientTwo.clientId.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/authorize?response_type=invalid-response&client_id=$clientId&scope=open&state=abc123")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          resp.body mustBe "InvalidResponseType"
        }
      }
    }
  }
}
