package models

import play.api.libs.json.{Json, Writes}

case class TokenResponse(accessToken: String,
                         idToken: String)

object TokenResponse {
  implicit val outboundWriter: Writes[TokenResponse] = (tokenResponse: TokenResponse) => Json.obj(
    "access_token" -> tokenResponse.accessToken,
    "id_token"     -> tokenResponse.idToken
  )
}
