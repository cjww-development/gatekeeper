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
import helpers.{Assertions, IntegrationApp, PlaySession}
import models.User
import org.jsoup.Jsoup
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class LoginPostApisISpec
  extends PlaySpec
    with IntegrationApp
    with PlaySession
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val individualUserStore: UserStore = app.injector.instanceOf[IndividualUserStore]
  lazy val orgUserStore: UserStore = app.injector.instanceOf[OrganisationUserStore]

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(individualUserStore.createUser(testIndividualUser))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
  }

  "POST /login" should {
    "return a See other" when {
      "the users credentials have been validated and have been redirected to MFA" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testUserName"),
            "password" -> Seq("testPassword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/login/mfa-challenge")
          assert(resp.cookie("att").isDefined)
        }
      }

      "the users credentials have been validated and have been redirected to MFA and there is a post login redirect" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testUserName"),
            "password" -> Seq("testPassword"),
            "redirect" -> Seq("/test-redirect"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/login/mfa-challenge?redirect=/test-redirect")
          assert(resp.cookie("att").isDefined)
        }
      }
    }

    "return a Bad request" when {
      "the form data is invalid" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "ussssserName" -> Seq("testUserName"),
            "passsssword" -> Seq("testPassword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("login-error").text() mustBe "Check your user name or password"
        }
      }

      "the users credentials are invalid" in {
        val result = ws
          .url(s"$testAppUrl/login")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testUserName"),
            "password" -> Seq("testPasssword")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("login-error").text() mustBe "Check your user name or password"
        }
      }
    }
  }
}