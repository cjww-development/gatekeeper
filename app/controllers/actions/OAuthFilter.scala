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

import org.slf4j.LoggerFactory
import pdi.jwt.{Jwt, JwtAlgorithm}
import play.api.libs.json.Json
import play.api.mvc.{BaseController, Request, Result}

import scala.concurrent.Future

trait OAuthFilter {
  self: BaseController =>

  val signature: String

  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateBearerToken(f: String => Future[Result])(implicit req: Request[_]): Future[Result] = {
    req.headers.get("Authorization").map(_.split(" ")(1)) match {
      case Some(token) => if(Jwt.isValid(token, signature, Seq(JwtAlgorithm.HS512))) {
        val (_, payload, _) = Jwt.decodeAll(token, signature, Seq(JwtAlgorithm.HS512)).get
        val json = Json.parse(payload.toJson)
        json.\("sub").asOpt[String] match {
          case Some(id) =>
            logger.info("[validateBearerToken] - Bearer token validated, proceeding")
            f(id)
          case None =>
            logger.warn("[validateBearerToken] - Could not find authorised user in token")
            Future.successful(Unauthorized(Json.obj(
              "msg" -> "Missing user authorisation"
            )))
        }
      } else {
        logger.warn("[validateBearerToken] - Bearer token was invalid")
        Future.successful(Unauthorized(Json.obj(
          "msg" -> "Invalid bearer token"
        )))
      }
      case None =>
        logger.warn("[validateBearerToken] - No Auth header provided")
        Future.successful(Unauthorized(Json.obj(
          "msg" -> "missing authorisation header"
        )))
    }
  }
}
