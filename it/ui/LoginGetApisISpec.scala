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

import scala.concurrent.ExecutionContext.Implicits.global

class LoginGetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val individualUserStore: UserStore = app.injector.instanceOf[IndividualUserStore]
  lazy val loginAttemptStore: LoginAttemptStore = app.injector.instanceOf[LoginAttemptStore]

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  val testMFAIndividualUser: User = User(
    userName = "testMfaUserName",
    email = "test-mfa@email.com",
    accType = "individual",
    password = "testPassword"
  ).copy(
    mfaEnabled = true,
    mfaSecret = Some("testSecret")
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(individualUserStore.createUser(testIndividualUser))
    await(individualUserStore.createUser(testMFAIndividualUser))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(loginAttemptStore.collection[LoginAttempt].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testMFAIndividualUser.id)).toFuture())
  }

  "GET /login" should {
    "return an Ok" when {
      "is not authenticated" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Sign in"
        }
      }
    }

    "return a See other" when {
      "the user is authenticated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/login")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account")
        }
      }
    }
  }

  "GET /login/mfa-challenge" should {
    "return an Ok" when {
      "the user is signing in and has MFA enabled on their account" in {
        val testLoginAttempt: LoginAttempt = LoginAttempt(
          id = "testLoginAttemptId",
          userId = testMFAIndividualUser.id,
          success = true,
          createdAt = DateTime.now()
        )

        await(loginAttemptStore.createLoginAttempt(testLoginAttempt))

        val authCookie: Cookie = ServerCookies.createAuthCookie(testMFAIndividualUser.id, enc = true)
        val reqAuthCookie: DefaultWSCookie = DefaultWSCookie(name = authCookie.name, value = authCookie.value)

        val loginCookie: Cookie = ServerCookies.createMFAChallengeCookie(testLoginAttempt.id, enc = true)
        val reqLoginCookie: DefaultWSCookie = DefaultWSCookie(name = loginCookie.name, value = loginCookie.value)

        val result = ws
          .url(s"$testAppUrl/login/mfa-challenge")
          .withCookies(reqAuthCookie, reqLoginCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Login challenge"
        }
      }
    }

    "return a See other" when {
      "the user is signing in and has MFA enabled on their account" in {
        val testLoginAttempt: LoginAttempt = LoginAttempt(
          id = "testLoginAttemptId",
          userId = testMFAIndividualUser.id,
          success = true,
          createdAt = DateTime.now()
        )

        await(loginAttemptStore.createLoginAttempt(testLoginAttempt))

        val authCookie: Cookie = ServerCookies.createAuthCookie(testMFAIndividualUser.id, enc = true)
        val reqAuthCookie: DefaultWSCookie = DefaultWSCookie(name = authCookie.name, value = authCookie.value)

        val loginCookie: Cookie = ServerCookies.createMFAChallengeCookie(testLoginAttempt.id, enc = true)
        val reqLoginCookie: DefaultWSCookie = DefaultWSCookie(name = loginCookie.name, value = loginCookie.value)

        val result = ws
          .url(s"$testAppUrl/login/mfa-challenge")
          .withCookies(reqAuthCookie, reqLoginCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Login challenge"
        }
      }
    }
  }

  "GET /logout" should {
    "return a See other" when {
      "the user has been logged out" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Sign in"
        }
      }
    }
  }
}