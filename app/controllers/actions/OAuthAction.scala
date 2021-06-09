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
import org.slf4j.LoggerFactory
import pdi.jwt.{Jwt, JwtAlgorithm}
import play.api.libs.json.Json
import play.api.mvc._
import services.TokenService

import scala.concurrent.{Future, ExecutionContext => ExC}

trait OAuthAction {
  self: BaseController =>

  val signature: String

  val tokenService: TokenService

  private val logger = LoggerFactory.getLogger(this.getClass)

  private type TokenInfo = Request[AnyContent] => String => Seq[String] => Future[Result]

  def authorised(f: TokenInfo)(implicit ec: ExC): Action[AnyContent] = Action.async { req =>
    req.headers.get("Authorization") match {
      case Some(header) =>
        val splitHeader = header.split(" ")
        (splitHeader.head == "Bearer", Jwt.isValid(splitHeader.last, signature, Seq(JwtAlgorithm.HS512))) match {
          case (true, true)  =>
            val (_, payload, _) = Jwt.decodeAll(splitHeader.last, signature, Seq(JwtAlgorithm.HS512)).get
            val json = Json.parse(payload.toJson)
            tokenService.lookupTokenRecordSet(json.\("tsid").as[String], json.\("sub").as[String]) flatMap {
              case Some(record) => if(record.accessTokenId == json.\("tid").as[String]) {
                logger.info(s"[validateAccessToken] - Token deemed valid, returning userId and scopes")
                f(req)(json.\("sub").as[String])(json.\("scp").as[String].split(","))
              } else {
                logger.warn(s"[validateAccessToken] - Token record found the access tid did not match")
                Future.successful(Unauthorized(StandardErrors.INVALID_TOKEN))
              }
              case None =>
                logger.warn(s"[validateAccessToken] - The tokens tsid was not found on record")
                Future.successful(Unauthorized(StandardErrors.INVALID_TOKEN))
            }
          case (_, _)        =>
            logger.warn(s"[validateAccessToken] - Auth header was found but was not in the valid format")
            Future.successful(Unauthorized(StandardErrors.INVALID_TOKEN))
        }
      case None =>
        logger.warn(s"[validateAccessToken] - No authorization header was present in the request")
        Future.successful(Unauthorized(StandardErrors.INVALID_TOKEN))
    }
  }
}
