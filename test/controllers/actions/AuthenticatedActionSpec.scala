/*
 * Copyright 2020 CJWW Development
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

package controllers.actions

import helpers.Assertions
import helpers.orchestrators.MockUserOrchestrator
import models._
import orchestrators.UserOrchestrator
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Results.Ok
import play.api.mvc.{BaseController, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthenticatedActionSpec extends PlaySpec with MockUserOrchestrator with Assertions {

  private val testFilter = new AuthenticatedAction with BaseController {
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
    override val userOrchestrator: UserOrchestrator = mockUserOrchestrator
  }

  private val okFunction: UserInfo => Future[Result] = user => Future.successful(Ok(s"I am user ${user.id}"))

  val now: DateTime = DateTime.now()

  val testIndUser: User = User(
    id        = s"user-${UUID.randomUUID()}",
    userName  = "testUsername",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com",
        verified = true
      ),
      phone = None
    ),
    profile = None,
    address = None,
    accType   = "individual",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  val testOrgUser: User = User(
    id        = s"org-user-${UUID.randomUUID()}",
    userName  = "testUsername",
    digitalContact = DigitalContact(
      email = Email(
        address = "test@email.com",
        verified = true
      ),
      phone = None
    ),
    profile = None,
    address = None,
    accType   = "organisation",
    password  = "testPassword",
    salt      = "testSalt",
    authorisedClients = List(),
    mfaSecret = None,
    mfaEnabled = false,
    createdAt = now
  )

  "authenticatedUser" should {
    "return an ok" when {
      "there is a valid cookie and user information was found" in {
        val req = FakeRequest()
          .withCookies(ServerCookies.createAuthCookie(testIndUser.id, enc = true))

        mockGetUserDetails(details = Option(UserInfo(
          id = testIndUser.id,
          userName = testIndUser.userName,
          email = testIndUser.digitalContact.email.address,
          emailVerified = testIndUser.digitalContact.email.verified,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = testIndUser.accType,
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)          mustBe OK
          contentAsString(res) mustBe s"I am user ${testIndUser.id}"
        }
      }
    }

    "return a redirect" when {
      "there is no valid cookie" in {
        val req = FakeRequest("GET", "/test-redirect")

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(s"${controllers.ui.routes.LoginController.loginShow().url}?redirect=%2Ftest-redirect")
        }
      }

      "there is a valid cookie but no user information" in {
        val req = FakeRequest("GET", "/test-redirect")
          .withCookies(ServerCookies.createAuthCookie("testUserId", enc = true))

        mockGetUserDetails(details = None)

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(s"${controllers.ui.routes.LoginController.loginShow().url}?redirect=%2Ftest-redirect")
        }
      }
    }
  }

  "authenticatedOrgUser" should {
    "return an ok" when {
      "there is a valid cookie and org user information was found" in {
        val req = FakeRequest()
          .withCookies(ServerCookies.createAuthCookie(testOrgUser.id, enc = true))

        mockGetUserDetails(details = Some(UserInfo(
          id = testOrgUser.id,
          userName = testOrgUser.userName,
          email = testOrgUser.digitalContact.email.address,
          emailVerified = testOrgUser.digitalContact.email.verified,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = testOrgUser.accType,
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        val result = testFilter.authenticatedOrgUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)          mustBe OK
          contentAsString(res) mustBe s"I am user ${testOrgUser.id}"
        }
      }
    }

    "return a Not found" when {
      "there is a valid cookie but no org user information was found" in {
        val req = FakeRequest()
          .withCookies(ServerCookies.createAuthCookie(testIndUser.id, enc = true))

        mockGetUserDetails(details = Some(UserInfo(
          id = testIndUser.id,
          userName = testIndUser.userName,
          email = testIndUser.digitalContact.email.address,
          emailVerified = testIndUser.digitalContact.email.verified,
          phone = None,
          phoneVerified = false,
          name = Name(
            firstName = None,
            middleName = None,
            lastName = None,
            nickName = None
          ),
          gender = Gender(
            selection = "not specified",
            custom = None
          ),
          address = None,
          birthDate = None,
          accType = "individual",
          authorisedClients = List.empty[AuthorisedClient],
          mfaEnabled = false,
          createdAt = now
        )))

        val result = testFilter.authenticatedOrgUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe NOT_FOUND
        }
      }
    }

    "return a redirect" when {
      "there is no valid cookie" in {
        val req = FakeRequest("GET", "/test-redirect")

        val result = testFilter.authenticatedOrgUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(s"${controllers.ui.routes.LoginController.loginShow().url}?redirect=%2Ftest-redirect")
        }
      }

      "there is a valid cookie but no user information" in {
        val req = FakeRequest("GET", "/test-redirect")
          .withCookies(ServerCookies.createAuthCookie("testUserId", enc = true))

        mockGetUserDetails(details = None)

        val result = testFilter.authenticatedOrgUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)           mustBe SEE_OTHER
          redirectLocation(res) mustBe Some(s"${controllers.ui.routes.LoginController.loginShow().url}?redirect=%2Ftest-redirect")
        }
      }
    }
  }
}
