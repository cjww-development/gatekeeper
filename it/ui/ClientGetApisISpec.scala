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
import models._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ClientGetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val orgUserStore: UserStore = app.injector.instanceOf[OrganisationUserStore]
  lazy val appStore: AppStore = app.injector.instanceOf[AppStore]
  lazy val tokenRecordStore: TokenRecordStore = app.injector.instanceOf[TokenRecordStore]

  val now: DateTime = DateTime.now()
  val uuid: String = UUID.randomUUID().toString

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
    iconUrl = None
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    await(orgUserStore.createUser(testOrganisationUser))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
    await(tokenRecordStore.collection[TokenRecord].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
  }

  "GET /clients" should {
    "return an Ok" when {
      "the org user is authenticated and their registered clients are shown but there are none" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/clients")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Registered clients"
          result.body().getElementById("no-clients").text() mustBe "You have no registered clients"
        }
      }

      "the org user is authenticated and their registered clients are shown" in {
        await(appStore.createApp(testClient))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/clients")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Registered clients"
          result.body().getElementById("app-0-name").text() mustBe testClient.name
          result.body().getElementById("app-0-desc").text() mustBe testClient.desc
        }
      }
    }
  }

  "GET /clients/authorised" should {
    "return an Ok" when {
      "the user is authenticated and their authorised apps are shown but there are none" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/clients/authorised")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Authorised apps"
          result.body().getElementById("no-authorised-apps").text() mustBe "You have no authorised apps"
        }
      }

      "the user is authenticated and their authorised apps are shown" in {
        val testOrganisationUserTwo: User = User(
          userName = "testOrgUserName2",
          email = "test-org2@email.com",
          accType = "organisation",
          password = "testPassword"
        ).copy(authorisedClients = List(
          AuthorisedClient(
            appId = testClient.appId,
            authorisedScopes = Seq(),
            authorisedOn = DateTime.now()
          )
        ))

        await(appStore.createApp(testClient))
        await(orgUserStore.createUser(testOrganisationUserTwo))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUserTwo.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/clients/authorised")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Authorised apps"
          result.body().getElementById("authorised-apps").text() mustBe "These are the apps you have given permission to, to access your data"
          result.body().getElementById("app-0-name").text() mustBe testClient.name
          result.body().getElementById("app-0-desc").text() mustBe testClient.desc
        }
      }
    }
  }

  "GET /client/authorised" should {
    "return an Ok" when {
      "the user is authenticated and the selected authorised app is shown" in {
        val testOrganisationUserThree: User = User(
          userName = "testOrgUserName3",
          email = "test-org3@email.com",
          accType = "organisation",
          password = "testPassword"
        ).copy(authorisedClients = List(
          AuthorisedClient(
            appId = testClient.appId,
            authorisedScopes = Seq(),
            authorisedOn = DateTime.now()
          )
        ))

        await(appStore.createApp(testClient))
        await(orgUserStore.createUser(testOrganisationUserThree))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUserThree.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/authorised?appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Authorised app"
          result.body().getElementById("name").text() mustBe testClient.name
          result.body().getElementById("desc").text() mustBe testClient.desc
          result.body().getElementById("owner").text() mustBe "testOrgUserName"
        }
      }
    }

    "return a Not found" when {
      "the user is authenticated and the selected app isn't authorised by the user" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/authorised?appId=invalid-app-id")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe NOT_FOUND
        }
      }
    }
  }

  "GET /client/revoke/app" should {
    "return a See other" when {
      "the user is authenticated and the selected authorised app is revoked" in {
        val testOrganisationUserFour: User = User(
          userName = "testOrgUserName4",
          email = "test-org4@email.com",
          accType = "organisation",
          password = "testPassword"
        ).copy(authorisedClients = List(
          AuthorisedClient(
            appId = testClient.appId,
            authorisedScopes = Seq(),
            authorisedOn = DateTime.now()
          )
        ))

        await(appStore.createApp(testClient))
        await(orgUserStore.createUser(testOrganisationUserFour))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUserFour.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/revoke/app?appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/clients/authorised")
        }

        awaitAndAssert(orgUserStore.findUser(mongoEqual("id", testOrganisationUserFour.id))) {
          _.get.authorisedClients mustBe List.empty[AuthorisedClient]
        }
      }
    }
  }

  "GET /client/revoke/session" should {
    "return a See other" when {
      "the user is authenticated and the selected authorised apps session is revoked" in {
        val testOrganisationUserFive: User = User(
          userName = "testOrgUserName5",
          email = "test-org5@email.com",
          accType = "organisation",
          password = "testPassword"
        ).copy(authorisedClients = List(
          AuthorisedClient(
            appId = testClient.appId,
            authorisedScopes = Seq(),
            authorisedOn = DateTime.now()
          )
        ))

        val testTokenRecord: TokenRecord = TokenRecord(
          tokenSetId = "testTokenSetId",
          userId = testOrganisationUserFive.id,
          appId = testClient.appId,
          accessTokenId = "testAccessTokenId",
          idTokenId = Some("testIdTokenId"),
          refreshTokenId = Some("testRefreshTokenId"),
          issuedAt = DateTime.now()
        )

        await(appStore.createApp(testClient))
        await(orgUserStore.createUser(testOrganisationUserFive))
        await(tokenRecordStore.createTokenRecord(testTokenRecord))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUserFive.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/revoke/session?tokenSetId=${testTokenRecord.tokenSetId}&appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some(s"/gatekeeper/client/authorised?appId=${testClient.appId}")
        }

        awaitAndAssert(tokenRecordStore.getActiveRecords(mongoEqual("tokenSetId", testTokenRecord.tokenSetId))) {
          _ mustBe Seq.empty[TokenRecord]
        }
      }
    }
  }

  "GET /client/register" should {
    "return an Ok" when {
      "the org user is authenticated and the client registration page is shown" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Register a Client"
        }
      }
    }
  }

  "GET /client" should {
    "return an Ok" when {
      "the org user is authenticated and the client registration page is shown" in {
         await(appStore.createApp(testClient))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client?appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Registered client"
          result.body().getElementById("name").text() mustBe testClient.name
          result.body().getElementById("desc").text() mustBe testClient.desc
        }
      }
    }
  }

  "GET /client/regenerate" should {
    "return an Ok" when {
      "the org user is authenticated and they're presented with the regenerate client page" in {
        await(appStore.createApp(testClient))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/regenerate?appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Regenerate client"
        }
      }
    }
  }

  "GET /client/delete" should {
    "return an Ok" when {
      "the org user is authenticated and they're presented with the regenerate client page" in {
        await(appStore.createApp(testClient))

        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/delete?appId=${testClient.appId}")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Delete client"
        }
      }
    }

    "return a Not found" when {
      "the org user is authenticated and but the selected app doesn't exist" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/delete?appId=invalid-app-id")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe NOT_FOUND
        }
      }
    }
  }
}
