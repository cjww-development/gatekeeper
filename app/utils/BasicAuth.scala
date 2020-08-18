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

package utils

import play.api.mvc.Request
import java.util.Base64
import java.nio.charset.StandardCharsets

import org.slf4j.LoggerFactory

import scala.util.Try

sealed trait BasicAuthResponse
case object NoAuthHeader extends BasicAuthResponse
case object InvalidPrefix extends BasicAuthResponse
case object MalformedHeader extends BasicAuthResponse

object BasicAuth extends BasicAuth

trait BasicAuth {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def decode(implicit req: Request[_]): Either[(String, String), BasicAuthResponse] = {
    logger.info(s"[decode] - Decoding Basic auth header on path ${req.path}")

    def noAuthHeader = {
      logger.warn(s"[decode] - No auth header found in the request")
      Right(NoAuthHeader)
    }

    req.headers.get("Authorization").fold[Either[(String, String), BasicAuthResponse]](noAuthHeader) { auth =>
      val splitHeader = auth.split(" ")
      if(splitHeader.head == "Basic") {
        logger.info(s"[decode] - Valid Basic auth header found, attempting decode")
        Try(Base64.getDecoder.decode(splitHeader.last)).fold(
          err => {
            logger.warn(s"[decode] - Auth header was found, but payload was not Base64", err)
            Right(MalformedHeader)
          },
          headerBytes => {
            val Array(user, pass) = new String(headerBytes, StandardCharsets.UTF_8).split(":")
            Left(user -> pass)
          }
        )
      } else {
        logger.warn(s"[decode] - Auth header was found, but invalid prefix for basic auth found (${splitHeader.head})")
        Right(InvalidPrefix)
      }
    }
  }
}
