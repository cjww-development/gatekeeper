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

import controllers.ui.routes
import models.ServerCookies._
import orchestrators.UserOrchestrator
import org.slf4j.LoggerFactory
import play.api.mvc._
import views.html.misc.{NotFound => NotFoundView}

import scala.concurrent.{Future, ExecutionContext => ExC}

trait AuthenticatedFilter {
  self: BaseController =>

  val userOrchestrator: UserOrchestrator

  private val logger = LoggerFactory.getLogger(this.getClass)

  private type AuthenticatedRequest = Request[AnyContent] => String => Future[Result]

  def authenticatedUser(f: AuthenticatedRequest)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    req.cookies.get("aas") match {
      case Some(cookie) =>
        val userId = cookie.getValue()
        userOrchestrator.getUserDetails(userId) flatMap {
          user => if(user.nonEmpty) {
            logger.info(s"[authenticatedUser] - Authenticated user found, authenticated as user $userId")
            f(req)(userId)
          } else {
            logger.warn(s"[authenticatedUser] - Authenticated user found, but could not find user on record")
            loginRedirect
          }
        }
      case None =>
        logger.warn(s"[authenticatedUser] - No authenticated user found, redirecting to login")
        loginRedirect
    }
  }

  def authenticatedOrgUser(f: AuthenticatedRequest)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    req.cookies.get("aas") match {
      case Some(cookie) =>
        val userId = cookie.getValue()
        userOrchestrator.getUserDetails(userId) flatMap {
          user => if(user.nonEmpty) {
            logger.info(s"[authenticatedUser] - Authenticated user found, authenticated as user $userId")
            if(user.get("accountType").contains("organisation")) {
              f(req)(userId)
            } else {
              logger.warn(s"[authenticatedUser] - Authenticated user found, but user is not an org user")
              Future.successful(NotFound(NotFoundView()))
            }
          } else {
            logger.warn(s"[authenticatedUser] - Authenticated user found, but could not find user on record")
            loginRedirect
          }
        }
      case None =>
        logger.warn(s"[authenticatedUser] - No authenticated user found, redirecting to login")
        loginRedirect
    }
  }

  private def loginRedirect(implicit req: Request[_]): Future[Result] = {
    Future.successful(Redirect(routes.LoginController.show().url, Map(
      "redirect" -> Seq(req.uri)
    )))
  }
}
