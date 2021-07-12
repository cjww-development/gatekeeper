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

import errors.StandardErrors
import models.RegisteredApplication
import org.slf4j.LoggerFactory
import play.api.mvc._
import services.oauth2.ClientService

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.util.Try

trait BasicAuthAction {
  self: BaseController =>

  val clientService: ClientService

  private val logger = LoggerFactory.getLogger(this.getClass)

  implicit val ec: ExC

  private type ClientAction = Request[AnyContent] => RegisteredApplication => Option[String] => Future[Result]

  def clientAuthenticationOptionalPkce(f: ClientAction)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    val clientCreds = getClientCredFromHeader.fold(getClientCredFromBody)(Some(_))
    val pkceVerifier = getPkceCodeVerifier
    clientCreds -> pkceVerifier match {
      case (None, Some((id, verifier))) =>  clientService.getRegisteredAppById(id) flatMap {
        case Some(app) =>
          val decodedApp = RegisteredApplication.decode(app)
          logger.info(s"[clientAuthentication] - Matched client with clientId $id")
          f(req)(decodedApp)(Some(verifier))
        case None =>
          logger.warn(s"[clientAuthentication] - No client has been found matching clientId $id")
          Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
      }
      case (Some((id, sec)), None) => clientService.getRegisteredAppByIdAndSecret(id, sec) flatMap {
        case Some(app) =>
          val decodedApp = RegisteredApplication.decode(app)
          logger.info(s"[clientAuthentication] - Matched client with clientId $id")
          f(req)(decodedApp)(None)
        case None =>
          logger.warn(s"[clientAuthentication] - No client has been found matching clientId $id")
          Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
      }
      case (None, None) =>
        Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
    }
  }

  def clientAuthentication(f: ClientAction)(implicit ec: ExC): Action[AnyContent] = Action.async { implicit req =>
    val clientCreds = getClientCredFromHeader.fold(getClientCredFromBody)(x => Some(x))
    clientCreds match {
      case Some((id, sec)) => clientService.getRegisteredAppByIdAndSecret(id, sec) flatMap {
        case Some(app) =>
          val decodedApp = RegisteredApplication.decode(app)
          logger.info(s"[clientAuthentication] - Matched client with clientId $id")
          f(req)(decodedApp)(None)
        case None =>
        logger.warn(s"[clientAuthentication] - No client has been found matching clientId $id")
        Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
      }
      case None =>
        Future.successful(Unauthorized(StandardErrors.INVALID_CLIENT))
    }
  }

  private def getClientCredFromBody(implicit req: Request[AnyContent]): Option[(String, String)] = {
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val clientId = body.getOrElse("client_id", Seq()).headOption
    val clientSec = body.getOrElse("client_secret", Seq()).headOption
    clientId -> clientSec match {
      case (Some(id), Some(sec)) =>
        logger.info(s"[clientAuthentication] - The client id and secret were found in the request body")
        Some((id, sec))
      case (_, _) =>
        logger.warn(s"[clientAuthentication] - Either the client Id or secret was not included in the request body")
        None
    }
  }

  private def getPkceCodeVerifier(implicit req: Request[AnyContent]): Option[(String, String)] = {
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val clientId = body.getOrElse("client_id", Seq()).headOption
    val codeVerifier = body.getOrElse("code_verifier", Seq()).headOption
    clientId -> codeVerifier match {
      case (Some(id), Some(sec)) =>
        logger.info(s"[clientAuthentication] - The client id and code verifier were found in the request body")
        Some((id, sec))
      case (_, _) =>
        logger.warn(s"[clientAuthentication] - Either the client Id or code verifier was not included in the request body")
        None
    }
  }

  private def getClientCredFromHeader(implicit req: Request[_]): Option[(String, String)] = {
    req.headers.get("Authorization").fold[Option[(String, String)]](noAuthHeader) { auth =>
      val splitHeader = auth.split(" ")
      if(splitHeader.head == "Basic") {
        Try(Base64.getDecoder.decode(splitHeader.last)).fold[Option[(String, String)]](
          err => {
            logger.warn(s"[clientAuthentication] - Basic auth header was found, but payload was not Base64", err)
            None
          },
          basicAuthHeader => {
            val Array(clientId, clientSecret) = new String(basicAuthHeader, StandardCharsets.UTF_8).split(":")
            Some((clientId, clientSecret))
          }
        )
      } else {
        logger.warn(s"[clientAuthentication] - Auth header wasn't of type Basic")
        None
      }
    }
  }

  private def noAuthHeader = {
    logger.warn(s"[clientAuthentication] - No auth header found in the request")
    None
  }
}
