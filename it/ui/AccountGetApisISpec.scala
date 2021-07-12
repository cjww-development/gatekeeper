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

import database.{CodecReg, IndividualUserStore, OrganisationUserStore, UserStore}
import helpers.{Assertions, IntegrationApp}
import models.{ServerCookies, User}
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

class AccountGetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val individualUserStore: UserStore = app.injector.instanceOf[IndividualUserStore]
  lazy val orgUserStore: UserStore = app.injector.instanceOf[OrganisationUserStore]

  val now: DateTime = DateTime.now()
  val uuid: String = UUID.randomUUID().toString

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  val testMFAUser: User = User(
    userName = "mfausername",
    email = "mfa@test-email.com",
    accType = "individual",
    password = "testPassword"
  ).copy(mfaEnabled = true)

  val testOrganisationUser: User = User(
    userName = "testOrgUserName",
    email = "test-org@email.com",
    accType = "organisation",
    password = "testPassword"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    await(individualUserStore.createUser(testIndividualUser))
    await(individualUserStore.createUser(testMFAUser))
    await(orgUserStore.createUser(testOrganisationUser))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testMFAUser.id)).toFuture())
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
  }

  "GET /account" should {
    "return an Ok" when {
      "the user is authenticated and the user details have been returned (individual)" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("user-name").text() mustBe "testUserName"
          result.body().getElementById("email").text() mustBe "test@email.com"
          result.body().getElementById("created-on").text() mustBe s"Member since ${DateTime.now().toString("yyyy-MM-dd")}"

          result.body().getElementById("email-verified").text() mustBe "You need to verify your email address"
          result.body().getElementById("mfa-enabled").text() mustBe "Enable two factor authentication"
          result.body().getElementById("authorised-client-count").text() mustBe "0"
        }
      }

      "the user is authenticated and the user details have been returned (organisation)" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testOrganisationUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("user-name").text() mustBe "testOrgUserName"
          result.body().getElementById("email").text() mustBe "test-org@email.com"
          result.body().getElementById("created-on").text() mustBe s"Member since ${DateTime.now().toString("yyyy-MM-dd")}"

          result.body().getElementById("email-verified").text() mustBe "You need to verify your email address"
          result.body().getElementById("mfa-enabled").text() mustBe "Enable two factor authentication"
          result.body().getElementById("authorised-client-count").text() mustBe "0"
        }
      }
    }
  }

  "GET /account/details" should {
    "return an Ok" when {
      "the user is authenticated and the account details page is presented" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Account details"
          result.body().getElementById("header-username").text() mustBe "testUserName"
          result.body().getElementById("username").text() mustBe "testUserName"
          result.body().getElementById("email").text() mustBe "test@email.com"
          result.body().getElementById("acc-type").text() mustBe "Individual"
          result.body().getElementById("member-since").text() mustBe s"${DateTime.now().toString("yyyy-MM-dd")}"
        }
      }
    }
  }

  "GET /account/security" should {
    "return an Ok" when {
      "the user is presented with the account security page" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("unverified-email").text() mustBe "test@email.com"
          result.body().getElementById("unverified-phone").text() mustBe "Enter your phone to further secure your account"
          result.body().getElementById("mfa-disabled").text() mustBe "Secure your account with two factor authentication"
        }
      }
    }
  }

  "GET /account/security/mfa-setup" should {
    "return an Ok" when {
      "the user is presented with totp setup page" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa-setup")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Enable two factor authentication"
          result.body().getElementById("card-title").text() mustBe "Scan your code"
          result.body().getElementById("card-lead").text() mustBe "Scan this code with your phones camera to receive codes in Google Authenticator"
        }
      }
    }

    "return a See other" when {
      "the user already has MFA enabled" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testMFAUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa-setup")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/security")
        }
      }
    }
  }

  "GET /account/security/mfa/confirm-disable" should {
    "return an Ok" when {
      "the user is presented with the mfa disable page" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testMFAUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa/confirm-disable")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Disable two factor authentication"
        }
      }
    }

    "return a See other" when {
      "the user doesn't have mfa enabled" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa/confirm-disable")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/security")
        }
      }
    }
  }

  "GET /account/security/mfa/disable" should {
    "return a See other" when {
      "the user has mfa enabled and has been then disabled" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testMFAUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa/disable")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/security")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testMFAUser.id))) {
          _.get.mfaEnabled mustBe false
        }
      }

      "the user doesn't have mfa enabled" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/mfa/disable")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/security")
        }
      }
    }
  }

  "GET /account/security/enter-phone-number" should {
    "return an Ok" when {
      "the user is presented with the enter your phone number page" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/enter-phone-number")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Enter your phone number"
        }
      }
    }
  }

  "GET /account/security/enter-verify-code" should {
    "return an Ok" when {
      "the user is presented with the enter your verification page" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/security/enter-verify-code")
          .withCookies(reqCookie)
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Enter your verification code"
        }
      }
    }
  }
}
