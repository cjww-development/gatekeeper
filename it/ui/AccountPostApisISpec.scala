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
import models.{ServerCookies, User}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._
import utils.StringUtils
import utils.StringUtils._

import java.time.temporal.ChronoUnit
import java.util.{Calendar, UUID}
import scala.concurrent.ExecutionContext.Implicits.global

class AccountPostApisISpec
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

  val now: DateTime = DateTime.now()
  val uuid: String = UUID.randomUUID().toString

  val testIndividualUser: User = User(
    userName = "testUserName",
    email = "test@email.com",
    accType = "individual",
    password = "testPassword"
  )

  val testIndividualUserOne: User = User(
    userName = "testOneUserName",
    email = "test-one@email.com",
    accType = "individual",
    password = "testPassword"
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def beforeEach(): Unit = {
    await(individualUserStore.createUser(testIndividualUser))
    await(individualUserStore.createUser(testIndividualUserOne))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUserOne.id)).toFuture())
  }

  "POST /account/details/update/email" should {
    "return a See other" when {
      "the users email address has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/email")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "email" -> Seq("updated-email@email.com")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) {
          _.get.digitalContact.email.address.decrypt.getOrElse("") mustBe "updated-email@email.com"
        }
      }
    }

    "return a Bad request" when {
      "the users email address has not been updated because the provided email is already being used" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/email")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "email" -> Seq("test-one@email.com")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "POST /account/details/update/password" should {
    "return a See other" when {
      "the users password has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/password")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "oldPassword" -> Seq("testPassword"),
            "newPassword" -> Seq("newPassword"),
            "confirmPassword" -> Seq("newPassword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) { user =>
          assert(user.get.password != StringUtils.hasher(testIndividualUser.salt, "testPassword"))
        }
      }
    }

    "return a Bad request" when {
      "the posted form data was invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/password")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "ollldPassword" -> Seq("testPassword"),
            "newPassssssword" -> Seq("newPassword"),
            "confirmmmmPassword" -> Seq("newPassword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }

      "the new and confirmed passwords don't match" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/password")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "oldPassword" -> Seq("testPassword"),
            "newPassword" -> Seq("newPassword"),
            "confirmPassword" -> Seq("newPasssssssword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }

      "the old password is invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/password")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken" -> Seq(csrfToken),
            "oldPassword" -> Seq("testPasswordddd"),
            "newPassword" -> Seq("newPassword"),
            "confirmPassword" -> Seq("newPassword"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "POST /account/details/update/name" should {
    "return a See other" when {
      "the users name has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/name")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "firstName"  -> Seq("testFirstName"),
            "middleName" -> Seq("testMiddleName"),
            "lastName"   -> Seq("testLastName"),
            "nickName"   -> Seq("testNickName")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) { user =>
          user.get.profile.get.givenName mustBe Some("testFirstName")
          user.get.profile.get.middleName mustBe Some("testMiddleName")
          user.get.profile.get.familyName mustBe Some("testLastName")
          user.get.profile.get.nickname mustBe Some("testNickName")
          user.get.profile.get.name mustBe Some("testFirstName testMiddleName testLastName")
        }
      }
    }
  }

  "POST /account/details/update/gender" should {
    "return a See other" when {
      "the users gender has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/gender")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "selection"  -> Seq("female")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) {
          _.get.profile.get.gender mustBe Some("female")
        }
      }
    }

    "return a Bad request" when {
      "the form data was invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/gender")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "selllllection"  -> Seq("female")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "POST /account/details/update/birthday" should {
    "return a See other" when {
      "the users birthday has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val calendar = Calendar.getInstance()
        calendar.set(2021, 0, 1, 0, 0, 0)

        val result = ws
          .url(s"$testAppUrl/account/details/update/birthday")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "birthday"  -> Seq("2021-01-01")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) { user =>
          val usersBirthday = user.get.profile.get.birthDate.get.toInstant.truncatedTo(ChronoUnit.DAYS)
          val dateToCompare = calendar.getTime.toInstant.truncatedTo(ChronoUnit.DAYS)
          assert(usersBirthday.equals(dateToCompare))
        }
      }
    }

    "return a Bad request" when {
      "the form data was invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/birthday")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "birthdayyy"  -> Seq("2021-01-01")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "POST /account/details/update/address" should {
    "return a See other" when {
      "the users address has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/address")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "streetAddress" -> Seq("testStreetAddress"),
            "locality" -> Seq("testLocality"),
            "region" -> Seq("testRegion"),
            "postalCode" -> Seq("testPostcode"),
            "country" -> Seq("testCountry")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe SEE_OTHER
          resp.header("Location") mustBe Some("/gatekeeper/account/details")
        }

        awaitAndAssert(individualUserStore.findUser(mongoEqual("id", testIndividualUser.id))) { user =>
          val address = user.get.address.get
          address.streetAddress mustBe "testStreetAddress"
          address.locality mustBe "testLocality"
          address.region mustBe "testRegion"
          address.postalCode mustBe "testPostcode"
          address.country mustBe "testCountry"
        }
      }
    }

    "return a Bad request" when {
      "the form data was invalid" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val result = ws
          .url(s"$testAppUrl/account/details/update/address")
          .withCookies(reqCookie, playCookie)
          .withFollowRedirects(follow = false)
          .post(Map(
            "csrfToken"  -> Seq(csrfToken),
            "streetAddressssss" -> Seq("testStreetAddress"),
            "locallllity" -> Seq("testLocality"),
            "reggggion" -> Seq("testRegion"),
            "postaaaaalCode" -> Seq("testPostcode"),
            "countttttry" -> Seq("testCountry"),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe BAD_REQUEST
        }
      }
    }
  }
}
