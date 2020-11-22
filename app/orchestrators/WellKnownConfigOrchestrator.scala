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

package orchestrators

import javax.inject.Inject
import models.WellKnownConfig
import play.api.Configuration

class DefaultWellKnownConfigOrchestrator @Inject()(val config: Configuration) extends WellKnownConfigOrchestrator {
  override val issuer: String = config.get[String]("well-known-config.issuer")

  override val authEndpoint: String = s"$issuer${controllers.ui.routes.OAuthController.authoriseGet("", "", "").url.split("\\?").head}"
  override val tokenEndpoint: String = s"$issuer${controllers.ui.routes.OAuthController.getToken().url.split("\\?").head}"
  override val revokeEndpoint: String = s"$issuer/api${controllers.api.routes.RevokationController.revokeToken().url.split("\\?").head}"
  override val userDetailsEndpoint: String = s"$issuer/api${controllers.api.routes.AccountController.getUserDetails().url.split("\\?").head}"
  override val jwksEndpoint: String = s"$issuer/api${controllers.api.routes.JwksController.getCurrentJwks().url.split("\\?").head}"

  override val grantTypes: Seq[String] = config.get[Seq[String]]("well-known-config.grant-types")
  override val supportedScopes: Seq[String] = config.get[Seq[String]]("well-known-config.scopes")
  override val responseTypes: Seq[String] = config.get[Seq[String]]("well-known-config.response-types")
  override val tokenEndpointAuth: Seq[String] = config.get[Seq[String]]("well-known-config.token-auth-method")

  override val idTokenAlgs: Seq[String] = config.get[Seq[String]]("well-known-config.id-token-algs")
}

trait WellKnownConfigOrchestrator {

  val issuer: String

  val authEndpoint: String
  val tokenEndpoint: String
  val revokeEndpoint: String
  val userDetailsEndpoint: String
  val jwksEndpoint: String

  val grantTypes: Seq[String]
  val supportedScopes: Seq[String]
  val responseTypes: Seq[String]
  val tokenEndpointAuth: Seq[String]

  val idTokenAlgs: Seq[String]

  def getConfig: WellKnownConfig = {
    WellKnownConfig(
      issuer,
      authorizationEndpoint = authEndpoint,
      tokenEndpoint = tokenEndpoint,
      userInfoEndpoint = userDetailsEndpoint,
      jwksUri = jwksEndpoint,
      registrationEndpoint = "",
      scopesSupported = supportedScopes,
      responseTypesSupported = responseTypes,
      grantTypesSupported = grantTypes,
      tokenEndpointAuth = tokenEndpointAuth,
      revokeEndpoint = revokeEndpoint,
      idTokenSigningAlgs = idTokenAlgs
    )
  }
}
