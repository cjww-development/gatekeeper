/*
 * Copyright 2022 CJWW Development
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
  override val grantTypes: Seq[String] = config.get[Seq[String]]("well-known-config.grant-types")
  override val supportedScopes: Seq[String] = config.get[Seq[String]]("well-known-config.scopes")
  override val responseTypes: Seq[String] = config.get[Seq[String]]("well-known-config.response-types")
  override val tokenEndpointAuth: Seq[String] = config.get[Seq[String]]("well-known-config.token-auth-method")
  override val idTokenAlgs: Seq[String] = config.get[Seq[String]]("well-known-config.id-token-algs")
}

trait WellKnownConfigOrchestrator {

  val grantTypes: Seq[String]
  val supportedScopes: Seq[String]
  val responseTypes: Seq[String]
  val tokenEndpointAuth: Seq[String]
  val idTokenAlgs: Seq[String]

  def getConfig(implicit rh: RequestHeader): WellKnownConfig = {
    val protocol = rh.headers.get("X-Forwarded-Proto").map(proto => s"$proto://").getOrElse("http://")
    val issuer = s"$protocol${rh.host}"

    WellKnownConfig(
      issuer,
      authorizationEndpoint = issuer + controllers.ui.routes.OAuthController.authoriseGet("", "", "").url.split("\\?").head,
      tokenEndpoint = issuer + controllers.ui.routes.OAuthController.getToken().url,
      userInfoEndpoint = issuer + controllers.api.routes.AccountController.getUserDetails.url,
      jwksUri = issuer + controllers.api.routes.JwksController.getCurrentJwks().url,
      registrationEndpoint = "",
      scopesSupported = supportedScopes,
      responseTypesSupported = responseTypes,
      grantTypesSupported = grantTypes,
      tokenEndpointAuth = tokenEndpointAuth,
      revokeEndpoint = issuer + controllers.api.routes.RevokationController.revokeToken().url,
      idTokenSigningAlgs = idTokenAlgs
    )
  }
}
