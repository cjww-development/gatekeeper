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
  override val authEndpoint: String = s"$issuer/api${controllers.ui.routes.OAuthController.authoriseGet("", "", "").url.split("\\?").head}"
  override val tokenEndpoint: String = s"$issuer/api${controllers.ui.routes.OAuthController.getToken().url.split("\\?").head}"
  override val grantTypes: Seq[String] = config.get[Seq[String]]("well-known-config.grant-types")
}

trait WellKnownConfigOrchestrator {

  val issuer: String
  val authEndpoint: String
  val tokenEndpoint: String
  val grantTypes: Seq[String]

  def getConfig: WellKnownConfig = {
    WellKnownConfig(
      issuer,
      authorizationEndpoint = authEndpoint,
      tokenEndpoint = tokenEndpoint,
      userInfoEndpoint = "",
      jwksUri = "",
      registrationEndpoint = "",
      scopesSupported = Seq(),
      responseTypesSupported = Seq(),
      grantTypesSupported = grantTypes,
      subjectTypesSupported = Seq(),
      idTokenSigningAlgValuesSupported = Seq()
    )
  }
}
