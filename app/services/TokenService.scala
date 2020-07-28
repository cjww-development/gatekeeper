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

package services

import java.time.{Clock, Instant}

import javax.inject.Inject
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import play.api.Configuration

class DefaultTokenService @Inject()(val config: Configuration) extends TokenService {
  override val issuer: String    = config.get[String]("jwt.iss")
  override val expiry: Long      = config.get[Long]("jwt.expiry")
  override val signature: String = config.get[String]("jwt.signature")
}

trait TokenService {

  val issuer: String
  val expiry: Long
  val signature: String

  private implicit val clock: Clock = Clock.systemUTC

  def createAccessToken(clientId: String, userId: String, scope: String): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by(issuer)
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusSeconds(expiry).getEpochSecond)
      .about(userId)
      .++[String](
        "scp" -> scope
      ).toJson

    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }

  def createIdToken(clientId: String, userId: String, user: Map[String, String], accType: String): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by(issuer)
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusSeconds(expiry).getEpochSecond)
      .about(userId)
      .++[String](
        "act" -> accType
      ).toJson

    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }
}
