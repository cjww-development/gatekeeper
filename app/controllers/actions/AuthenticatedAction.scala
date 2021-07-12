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

package controllers.actions

import controllers.ui.routes
import models.ServerCookies._
import models.UserInfo
import orchestrators.UserOrchestrator
import org.slf4j.LoggerFactory
import play.api.mvc._
import views.html.misc.{NotFound => NotFoundView}

import scala.concurrent.{Future, ExecutionContext => ExC}

trait AuthenticatedAction {
  self: BaseController =>

  val userOrchestrator: UserOrchestrator

  private val logger = LoggerFactory.getLogger(this.getClass)

  private type AuthenticatedRequest = Request[AnyContent] => String => Future[Result]
  private type AuthenticatedUserRequest = Request[AnyContent] => UserInfo => Future[Result]

  def authenticatedUser(f: AuthenticatedUserRequest)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    req.cookies.get("aas") match {
      case Some(cookie) =>
        val userId = cookie.getValue()
        userOrchestrator.getUserDetails(userId) flatMap {
          case Some(user) =>
            logger.info(s"[authenticatedUserWithUserInfo] - Authenticated user found, authenticated as user $userId")
            f(req)(user)
          case None =>
            logger.warn(s"[authenticatedUserWithUserInfo] - Authenticated user found, but could not find user on record")
            loginRedirect
        }
      case None =>
        logger.warn(s"[authenticatedUserWithUserInfo] - No authenticated user found, redirecting to login")
        loginRedirect
    }
  }

  def authenticatedOrgUser(f: AuthenticatedUserRequest)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    req.cookies.get("aas") match {
      case Some(cookie) =>
        val userId = cookie.getValue()
        userOrchestrator.getUserDetails(userId) flatMap {
          case Some(user) =>
            logger.info(s"[authenticatedOrgUserWithUserInfo] - Authenticated user found, authenticated as user $userId")
            if(user.accType == "organisation") {
              f(req)(user)
            } else {
              logger.warn(s"[authenticatedOrgUserWithUserInfo] - Authenticated user found, but user is not an org user")
              Future.successful(NotFound(NotFoundView()))
            }
          case None =>
            logger.warn(s"[authenticatedOrgUserWithUserInfo] - Authenticated user found, but could not find user on record")
            loginRedirect
        }
      case None =>
        logger.warn(s"[authenticatedOrgUserWithUserInfo] - No authenticated user found, redirecting to login")
        loginRedirect
    }
  }

  private def loginRedirect(implicit req: Request[_]): Future[Result] = {
    Future.successful(Redirect(routes.LoginController.loginShow().url, Map(
      "redirect" -> Seq(req.uri)
    )))
  }
}
