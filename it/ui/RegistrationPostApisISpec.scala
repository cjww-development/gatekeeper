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
import utils.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationPostApisISpec
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
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(individualUserStore.collection[User].deleteOne(mongoEqual("userName", "testUsername".encrypt)).toFuture())
    await(orgUserStore.collection[User].deleteOne(mongoEqual("userName", "testOrgUsername".encrypt)).toFuture())
  }

  "POST /register" should {
    "return an Ok" when {
      "an individual user has been registered" in {
        val result = ws
          .url(s"$testAppUrl/register")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testUsername"),
            "email" -> Seq("test@email.com"),
            "accType" -> Seq("individual"),
            "password" -> Seq("testPassword")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Welcome"
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("userName", "testUsername".encrypt))) {
          _.isDefined mustBe true
        }
      }

      "an organisation user has been registered" in {
        val result = ws
          .url(s"$testAppUrl/register")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testOrgUsername"),
            "email" -> Seq("test-org@email.com"),
            "accType" -> Seq("organisation"),
            "password" -> Seq("testPassword")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Welcome"
        }

        awaitAndAssert(orgUserStore.findUser(mongoEqual("userName", "testOrgUsername".encrypt))) {
          _.isDefined mustBe true
        }
      }
    }

    "return a Bad request" when {
      "the form data is invalid" in {
        val result = ws
          .url(s"$testAppUrl/register")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userrrrrName" -> Seq("testUsername"),
            "emaillll" -> Seq("test@email.com"),
            "accccccType" -> Seq("individual"),
            "passssssword" -> Seq("testPassword")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }

      "an organisation user has been registered" in {
        await(individualUserStore.createUser(testIndividualUser))

        val result = ws
          .url(s"$testAppUrl/register")
          .withCookies(playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "userName" -> Seq("testUserName"),
            "email" -> Seq("test@email.com"),
            "accType" -> Seq("individual"),
            "password" -> Seq("testPassword")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Sign up"
          result.body().getElementById("form-error").text() mustBe "Please use a different user name or email"
        }
      }
    }
  }
}
