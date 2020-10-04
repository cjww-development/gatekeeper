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

import java.nio.charset.StandardCharsets
import java.util.Base64

import errors.StandardErrors
import models.RegisteredApplication
import org.slf4j.LoggerFactory
import play.api.mvc.{Action, AnyContent, BaseController, Request, Result}
import services.ClientService

import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.util.Try

trait BasicAuthAction {
  self: BaseController =>

  val clientService: ClientService

  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit val ec: ExC

  private type ClientAction = Request[AnyContent] => RegisteredApplication => Future[Result]

  private def noAuthHeader = {
    logger.warn(s"[clientAuthentication] - No auth header found in the request")
    Future.successful(Unauthorized(StandardErrors.INVALID_REQUEST))
  }

  def clientAuthentication[T](f: ClientAction)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    req.headers.get("Authorization").fold(noAuthHeader) { auth =>
      val splitHeader = auth.split(" ")
      if(splitHeader.head == "Basic") {
        Try(Base64.getDecoder.decode(splitHeader.last)).fold(
          err => {
            logger.warn(s"[clientAuthentication] - Basic auth header was found, but payload was not Base64", err)
            Future.successful(Unauthorized(StandardErrors.INVALID_REQUEST))
          },
          basicAuthHeader => {
            val Array(clientId, clientSecret) = new String(basicAuthHeader, StandardCharsets.UTF_8).split(":")
            clientService.getRegisteredAppByIdAndSecret(clientId, clientSecret) flatMap {
              case Some(app) =>
                logger.info(s"[clientAuthentication] - Matched client with clientId $clientId")
                f(req)(app)
              case None =>
                logger.warn(s"[clientAuthentication] - No client has been found matching clientId $clientId")
                Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
            }
          }
        )
      } else {
        logger.warn(s"[clientAuthentication] - Auth header wasn't of type Basic")
        Future.successful(Unauthorized(StandardErrors.INVALID_REQUEST))
      }
    }
  }
}
