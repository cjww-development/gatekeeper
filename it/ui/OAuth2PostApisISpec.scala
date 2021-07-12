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
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.{DefaultWSCookie, WSAuthScheme, WSClient}
import play.api.mvc.Cookie
import play.api.test.Helpers._
import utils.StringUtils._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class OAuth2PostApisISpec
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
  lazy val grantStore: GrantStore = app.injector.instanceOf[GrantStore]

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
    clientId = RegisteredApplication.generateIds(iterations = 0).encrypt,
    clientSecret = Some(RegisteredApplication.generateIds(iterations = 1).encrypt),
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

  val testAuthCode: String = UUID.randomUUID().toString

  val testGrant: Grant = Grant(
    responseType = "code",
    authCode = testAuthCode,
    scope = Seq("openid"),
    clientId = testClient.clientId.decrypt.getOrElse(""),
    userId = testIndividualUser.id,
    accType = testIndividualUser.accType,
    redirectUri = testClient.redirectUrl,
    codeVerifier = None,
    codeChallenge = None,
    codeChallengeMethod = None,
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
    await(grantStore.createGrant(testGrant))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(individualUserStore.collection[User].drop().toFuture())
    await(orgUserStore.collection[User].drop().toFuture())
    await(appStore.collection[RegisteredApplication].drop().toFuture())
    await(grantStore.collection[Grant].drop().toFuture())
  }

  override def afterEach(): Unit = {
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUser.id)).toFuture())
    await(individualUserStore.collection[User].deleteOne(mongoEqual("id", testIndividualUserTwo.id)).toFuture())
    await(orgUserStore.collection[User].deleteOne(mongoEqual("id", testOrganisationUser.id)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClient.appId)).toFuture())
    await(appStore.collection[RegisteredApplication].deleteOne(mongoEqual("appId", testClientTwo.appId)).toFuture())
    await(grantStore.collection[Grant].deleteOne(mongoEqual("userUd", testIndividualUser.id)).toFuture())
  }

  "POST /oauth2/token" should {
    "return an Ok" when {
      "the user exchanges an auth code for an access token" in {
        val cookie: Cookie = ServerCookies.createAuthCookie(testIndividualUser.id, enc = true)
        val reqCookie: DefaultWSCookie = DefaultWSCookie(name = cookie.name, value = cookie.value)

        val clientId = testClient.clientId.decrypt.getOrElse("")
        val clientSecret = testClient.clientSecret.get.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/token")
          .withCookies(reqCookie)
          .withAuth(clientId, clientSecret, WSAuthScheme.BASIC)
          .withFollowRedirects(follow = false)
          .post(Map(
            "grant_type" -> Seq("authorization_code"),
            "code" -> Seq(testGrant.authCode),
            "redirect_uri" -> Seq(testGrant.redirectUri),
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          resp.json.\("token_type").as[String] mustBe "Bearer"
          resp.json.\("scope").as[String] mustBe "openid"
          assert(resp.json.\("access_token").asOpt[String].isDefined)
          assert(resp.json.\("id_token").asOpt[String].isDefined)
        }
      }

      "a machine requests an access token with client credentials" in {
        val clientId = testClient.clientId.decrypt.getOrElse("")
        val clientSecret = testClient.clientSecret.get.decrypt.getOrElse("")

        val result = ws
          .url(s"$testAppUrl/oauth2/token")
          .withAuth(clientId, clientSecret, WSAuthScheme.BASIC)
          .withFollowRedirects(follow = false)
          .post(Map(
            "grant_type" -> Seq("client_credentials"),
            "scope" -> Seq("read:machine-state")
          ))

        awaitAndAssert(result) { resp =>
          resp.status mustBe OK
          resp.json.\("token_type").as[String] mustBe "Bearer"
          resp.json.\("scope").as[String] mustBe "read:machine-state"
          assert(resp.json.\("access_token").asOpt[String].isDefined)
          assert(resp.json.\("id_token").asOpt[String].isEmpty)
        }
      }
    }
  }
}
