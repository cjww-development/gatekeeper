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

package orchestrators

import models.WellKnownConfig
import play.api.Configuration
import play.api.mvc.RequestHeader

import javax.inject.Inject

class DefaultWellKnownConfigOrchestrator @Inject()(val config: Configuration) extends WellKnownConfigOrchestrator {
  override val authEndpoint: RequestHeader => String = rh => controllers.ui.routes.OAuthController.authoriseGet("", "", "").absoluteURL()(rh).split("\\?").head
  override val tokenEndpoint: RequestHeader => String = rh => controllers.ui.routes.OAuthController.getToken().absoluteURL()(rh).split("\\?").head
  override val revokeEndpoint: RequestHeader => String = rh => controllers.api.routes.RevokationController.revokeToken().absoluteURL()(rh).split("\\?").head
  override val userDetailsEndpoint: RequestHeader => String = rh => controllers.api.routes.AccountController.getUserDetails.absoluteURL()(rh).split("\\?").head
  override val jwksEndpoint: RequestHeader => String = rh => controllers.api.routes.JwksController.getCurrentJwks().absoluteURL()(rh).split("\\?").head

  override val grantTypes: Seq[String] = config.get[Seq[String]]("well-known-config.grant-types")
  override val supportedScopes: Seq[String] = config.get[Seq[String]]("well-known-config.scopes")
  override val responseTypes: Seq[String] = config.get[Seq[String]]("well-known-config.response-types")
  override val tokenEndpointAuth: Seq[String] = config.get[Seq[String]]("well-known-config.token-auth-method")

  override val idTokenAlgs: Seq[String] = config.get[Seq[String]]("well-known-config.id-token-algs")
}

trait WellKnownConfigOrchestrator {
  val authEndpoint: RequestHeader => String
  val tokenEndpoint: RequestHeader => String
  val revokeEndpoint: RequestHeader => String
  val userDetailsEndpoint: RequestHeader => String
  val jwksEndpoint: RequestHeader => String

  val grantTypes: Seq[String]
  val supportedScopes: Seq[String]
  val responseTypes: Seq[String]
  val tokenEndpointAuth: Seq[String]

  val idTokenAlgs: Seq[String]

  def getConfig(implicit rh: RequestHeader): WellKnownConfig = {
    val protocol = if(rh.secure) "https://" else "http://"
    WellKnownConfig(
      s"$protocol${rh.host}",
      authorizationEndpoint = authEndpoint(rh),
      tokenEndpoint = tokenEndpoint(rh),
      userInfoEndpoint = userDetailsEndpoint(rh),
      jwksUri = jwksEndpoint(rh),
      registrationEndpoint = "",
      scopesSupported = supportedScopes,
      responseTypesSupported = responseTypes,
      grantTypesSupported = grantTypes,
      tokenEndpointAuth = tokenEndpointAuth,
      revokeEndpoint = revokeEndpoint(rh),
      idTokenSigningAlgs = idTokenAlgs
    )
  }
}
