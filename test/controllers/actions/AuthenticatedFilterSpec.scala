package controllers.actions

import helpers.Assertions
import helpers.orchestrators.MockUserOrchestrator
import models.ServerCookies
import orchestrators.UserOrchestrator
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{BaseController, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthenticatedFilterSpec extends PlaySpec with MockUserOrchestrator with Assertions {

  private val testFilter = new AuthenticatedFilter with BaseController {
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
    override val userOrchestrator: UserOrchestrator = mockUserOrchestrator
  }

  private val okFunction: String => Future[Result] = userId => Future.successful(Ok(s"I am user $userId"))

  "authenticatedUser" should {
    "return an ok" when {
      "there is a valid cookie and user information was found" in {
        val req = FakeRequest()
          .withCookies(ServerCookies.createAuthCookie("testUserId", enc = true))

        mockGetUserDetails(details = Map("userId" -> "testUserId"))

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res)          mustBe OK
          contentAsString(res) mustBe "I am user testUserId"
        }
      }
    }

    "return a forbidden" when {
      "there is no valid cookie" in {
        val req = FakeRequest()

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe FORBIDDEN
        }
      }

      "there is a valid cookie but no user information" in {
        val req = FakeRequest()
          .withCookies(ServerCookies.createAuthCookie("testUserId", enc = true))

        mockGetUserDetails(details = Map())

        val result = testFilter.authenticatedUser {
          _ => user => okFunction(user)
        }.apply(req)

        assertOutput(result) { res =>
          status(res) mustBe FORBIDDEN
        }
      }
    }
  }
}
