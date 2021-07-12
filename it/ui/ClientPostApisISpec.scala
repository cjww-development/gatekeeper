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

import database.{AppStore, CodecReg, OrganisationUserStore, UserStore}
import helpers.{Assertions, IntegrationApp, PlaySession}
import models.{RegisteredApplication, ServerCookies, User}
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class ClientPostApisISpec
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
    await(appStore.createApp(testClient))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
  }

  "POST /client/register" should {
    "return a See other" when {
      "the new client has been registered" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "name" -> Seq("test-name"),
            "desc" -> Seq("test desc"),
            "homeUrl" -> Seq("http://localhost:1234"),
            "redirectUrl" -> Seq("http://localhost:1234/redirect"),
            "clientType" -> Seq("confidential")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }
      }
    }

    "return a Bad request" when {
      "the form data is invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "nameee" -> Seq("test-name"),
            "desssc" -> Seq("test desc"),
            "homeeeUrl" -> Seq("http://localhost:1234"),
            "reddddirectUrl" -> Seq("http://localhost:1234/redirect"),
            "clienttttType" -> Seq("confidential")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "POST /client/register-preset" should {
    "return a See other" when {
      "the new preset client has been registered" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register-preset")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "preset-choice" -> Seq("jenkins")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }
      }
    }

    "return an Internal server error" when {
      "the form data is invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register-preset")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "presett-choice" -> Seq("jenkins")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe INTERNAL_SERVER_ERROR
        }
      }

      "the preset choice is invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/register-preset")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "preset-choice" -> Seq("invalid")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  "POST /client/regenerate" should {
    "return a See other" when {
      "the clients Id and secret have been regenerated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/regenerate?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken)
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          assert(app.get.clientId != testClient.clientId)
          assert(app.get.clientSecret != testClient.clientSecret)
        }
      }
    }

    "return a Not found" when {
      "the client doesn't exist" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/regenerate?appId=invalid-appId")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken)
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe NOT_FOUND
        }
      }
    }
  }

  "POST /client/delete" should {
    "return a See other" when {
      "the client has been deleted" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/delete?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken)
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          assert(app.isEmpty)
        }
      }
    }
  }

  "POST /client/update/flows" should {
    "return a See other" when {
      "the clients valid flows have been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/update/flows?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "auth-code-check" -> Seq("authorization_code"),
            "client-cred-check" -> Seq("client_credentials"),
            "refresh-check" -> Seq("refresh_token"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          app.get.oauth2Flows mustBe List("authorization_code", "client_credentials", "refresh_token")
        }
      }
    }
  }

  "POST /client/update/scopes" should {
    "return a See other" when {
      "the clients valid scopes have been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/update/scopes?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "openid-check" -> Seq("openid"),
            "profile-check" -> Seq("profile"),
            "email-check" -> Seq("email"),
            "address-check" -> Seq("address"),
            "phone-check" -> Seq("phone"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          app.get.oauth2Scopes mustBe List("openid", "profile", "email", "address", "phone")
        }
      }
    }
  }

  "POST /client/update/expiry" should {
    "return a See other" when {
      "the clients valid expiries have been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/update/expiry?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "id-token-mins" -> Seq("1"),
            "id-token-days" -> Seq("1"),
            "access-token-days" -> Seq("1"),
            "access-token-days" -> Seq("1"),
            "refresh-token-days" -> Seq("1"),
            "refresh-token-days" -> Seq("1"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          app.get.idTokenExpiry.toLong mustBe 86460000L
          app.get.accessTokenExpiry.toLong mustBe 86400000L
          app.get.refreshTokenExpiry.toLong mustBe 86400000L
        }
      }
    }
  }

  "POST /client/update/urls" should {
    "return a See other" when {
      "the clients valid urls have been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/update/urls?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "home-url" -> Seq("http://localhost:1234/home"),
            "redirect-url" -> Seq("http://localhost:1234/redirect")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          app.get.homeUrl mustBe "http://localhost:1234/home"
          app.get.redirectUrl mustBe "http://localhost:1234/redirect"
        }
      }
    }
  }

  "POST /client/update/basics" should {
    "return a See other" when {
      "the clients basic details have been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/client/update/basics?appId=${testClient.appId}")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "name" -> Seq("new-name"),
            "desc" -> Seq("new desc"),
            "icon-url" -> Seq("http://localhost:1234/icon"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          assert(resp.header("Location").get.contains("/gatekeeper/client"))
        }

        awaitAndAssert(appStore.validateAppOn(mongoEqual("appId", testClient.appId))) { app =>
          app.get.name mustBe "new-name"
          app.get.desc mustBe "new desc"
          app.get.iconUrl mustBe Some("http://localhost:1234/icon")
        }
      }
    }
  }
}
