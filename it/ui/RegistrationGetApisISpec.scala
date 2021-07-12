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
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationGetApisISpec
  extends PlaySpec
    with IntegrationApp
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg
    with Assertions {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val individualUserStore: UserStore = app.injector.instanceOf[IndividualUserStore]
  lazy val verificationStore: VerificationStore = app.injector.instanceOf[VerificationStore]

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  val testPayload: Verification = Verification(
    verificationId = "testVerificationId",
    userId = testIndividualUser.id,
    contactType = "email",
    contact = "test@email.com",
    code = None,
    accType = "individual",
    createdAt = DateTime.now()
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
    await(verificationStore.collection[Verification].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(verificationStore.collection[Verification].deleteOne(mongoEqual("verificationId", testPayload.verificationId)).toFuture())
  }

  "GET /register" should {
    "return an Ok" when {
      "some user has accessed the registration page" in {
        val result = ws
          .url(s"$testAppUrl/register")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Sign up"
        }
      }
    }
  }

  "GET /register/email-verification" should {
    "return an Ok" when {
      "the users email address has been successfully validated" in {
        await(individualUserStore.createUser(testIndividualUser))
        await(verificationStore.createVerificationRecord(testPayload))

        val result = ws
          .url(s"$testAppUrl/register/email-verification?payload=${Verification.obfuscator.encrypt(testPayload)}")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          val result = Jsoup.parse(resp.body)
          result.body().getElementById("title").text() mustBe "Email verified"
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) {
          _.get.digitalContact.email.verified mustBe true
        }
      }
    }

    "return a Not found" when {
      "the payload was valid but the verification record could not be found" in {
        val testPayload = Verification(
          verificationId = "testVerificationId",
          userId = "testUserId",
          contactType = "email",
          contact = "test@email.com",
          code = None,
          accType = "individual",
          createdAt = DateTime.now()
        )

        val result = ws
          .url(s"$testAppUrl/register/email-verification?payload=${Verification.obfuscator.encrypt(testPayload)}")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe NOT_FOUND
        }
      }
    }

    "return a Bad request" when {
      "the email payload was invalid" in {
        val result = ws
          .url(s"$testAppUrl/register/email-verification?payload=invalid-payload")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
          resp.json.\("error").as[String] mustBe "invalid_request"
        }
      }
    }
  }
}