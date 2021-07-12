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

package helpers

import play.api.Configuration
import play.api.http.{JWTConfiguration, SecretConfiguration}
import play.api.libs.ws.DefaultWSCookie
import play.api.mvc.JWTCookieDataCodec
import play.filters.csrf.CSRFFilter

trait PlaySession {
  self: IntegrationApp =>

  lazy val config: Configuration = app.injector.instanceOf[Configuration]
  lazy val csrfFilter: CSRFFilter = app.injector.instanceOf[CSRFFilter]

  val jwtCookie: JWTCookieDataCodec = new JWTCookieDataCodec {
    override def secretConfiguration: SecretConfiguration = SecretConfiguration(secret = config.get[String]("play.http.secret.key"))
    override def jwtConfiguration: JWTConfiguration = JWTConfiguration()
  }

  val csrfToken: String = csrfFilter.tokenProvider.generateToken
  val playCookie: DefaultWSCookie = DefaultWSCookie(name = "PLAY_SESSION", value = jwtCookie.encode(Map("csrfToken" -> csrfToken)))
}
