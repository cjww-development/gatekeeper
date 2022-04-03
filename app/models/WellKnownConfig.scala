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

package models

import play.api.libs.json.{Json, Writes}

case class WellKnownConfig(issuer: String,
                           authorizationEndpoint: String,
                           tokenEndpoint: String,
                           userInfoEndpoint: String,
                           revokeEndpoint: String,
                           jwksUri: String,
                           registrationEndpoint: String,
                           scopesSupported: Seq[String],
                           responseTypesSupported: Seq[String],
                           grantTypesSupported: Seq[String],
                           tokenEndpointAuth: Seq[String],
                           idTokenSigningAlgs: Seq[String])

object WellKnownConfig {
  implicit val writer: Writes[WellKnownConfig] = (wkc: WellKnownConfig) => Json.obj(
    "issuer"                                     -> wkc.issuer,
    "authorization_endpoint"                     -> wkc.authorizationEndpoint,
    "token_endpoint"                             -> wkc.tokenEndpoint,
    "userinfo_endpoint"                          -> wkc.userInfoEndpoint,
    "jwks_uri"                                   -> wkc.jwksUri,
    "scopes_supported"                           -> wkc.scopesSupported,
    "response_types_supported"                   -> wkc.responseTypesSupported,
    "grant_types_supported"                      -> wkc.grantTypesSupported,
    "id_token_signing_alg_values_supported"      -> wkc.idTokenSigningAlgs,
    "token_endpoint_auth_methods_supported"      -> wkc.tokenEndpointAuth,
    "revocation_endpoint"                        -> wkc.revokeEndpoint,
    "revocation_endpoint_auth_methods_supported" -> wkc.tokenEndpointAuth
  )
}
