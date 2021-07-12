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
import play.api.libs.ws.{DefaultWSCookie, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._
import services.oauth2.TokenService
import utils.StringUtils
import utils.StringUtils._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UserinfoGetApisISpec
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
  lazy val appStore: AppStore = app.injector.instanceOf[AppStore]

  lazy val tokenService: TokenService = app.injector.instanceOf[TokenService]

  val uuid: String = UUID.randomUUID().toString
  val now: DateTime = DateTime.now()
  val saltString: String = StringUtils.salter(length = 32)

  val testIndividualUser: User = User(
    id = s"user-$uuid",
    userName = "testUserName",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com".trim.encrypt,
        verified = true
      ),
      phone = Some(Phone(
        number = "testPhoneNumber",
        verified = true
      ))
    ),
    profile = Some(Profile(
      name = Some("firstname middlename lastname"),
      familyName = Some("lastname"),
      givenName = Some("firstname"),
      middleName = Some("middlename"),
      nickname = Some("nickname"),
      profile = Some("testProfile"),
      picture = Some("testPictureLink"),
      website = Some("testWebsite"),
      gender = Some("male"),
      birthDate = Some(now.toDate),
      zoneinfo = Some("testZone"),
      locale = Some("testLocale"),
      updatedAt = Some(now)
    )),
    address = Some(Address(
      formatted = "testFormatted",
      streetAddress = "testStreetAddress",
      locality = "testLocality",
      region = "testRegion",
      postalCode = "testPostcode",
      country = "testCountry"
    )),
    accType = "individual",
    salt = saltString,
    password = StringUtils.hasher(saltString, "testPassword"),
    authorisedClients = List.empty[AuthorisedClient],
    mfaEnabled = false,
    mfaSecret = None,
    createdAt = now
  )

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
    super.beforeEach()
    await(individualUserStore.createUser(testIndividualUser))
    await(orgUserStore.createUser(testOrganisationUser))
    await(appStore.createApp(testClient))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.afterEach()
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
  }

  "POST /api/oauth2/userinfo" should {
    "return a See other" when {
      "the users email address has been updated" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")

        val accessToken = await(for {
          token <- Future.successful(tokenService.createAccessToken(clientId, testIndividualUser.id, uuid, uuid, "openid,profile,email,phone,address", expiry = 900000))
          _     <- tokenService.createTokenRecordSet(uuid, testIndividualUser.id, testClient.appId, uuid, None, None)
        } yield {
          token
        })

        val result = ws
          .url(s"$testAppUrl/api/oauth2/userinfo")
          .withCookies(reqCookie, playCookie)
          .withHttpHeaders("Authorization" -> s"Bearer $accessToken")
          .withFollowRedirects(follow = false)
          .get()

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
        }
      }
    }
  }
}
