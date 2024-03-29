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

package services.oauth2

import com.nimbusds.jose.jwk.RSAKey
import database.TokenRecordStore
import dev.cjww.mongo.responses._
import models.{RefreshToken, TokenExpiry, TokenRecord}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.{combine, set}
import org.slf4j.LoggerFactory
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import play.api.Configuration
import play.api.libs.json.Json

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultTokenService @Inject()(val config: Configuration,
                                    val jwksService: JwksService,
                                    val tokenRecordStore: TokenRecordStore) extends TokenService {
  override val issuer: String    = config.get[String]("jwt.iss")
  override val expiry: Long      = config.get[Long]("jwt.expiry")
  override val signature: String = config.get[String]("jwt.signature")
  override val jwks: RSAKey = jwksService.getCurrentJwks
}

trait TokenService {

  val tokenRecordStore: TokenRecordStore

  val issuer: String
  val expiry: Long
  val signature: String

  val jwks: RSAKey

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def buildGenericPayload(aud: String, sub: String, expiry: Long, customClaims: List[(String, String)]): String = {
    val now = Instant.now
    JwtClaim()
      .to(aud)
      .by(issuer)
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusMillis(expiry).getEpochSecond)
      .about(sub)
      .++[String](customClaims:_*)
      .toJson
  }

  def createAccessToken(clientId: String, userId: String, setId: String, tokenId: String, scope: String, expiry: Long): String = {
    val customClaims = List("scp" -> scope, "tsid" -> setId, "tid" -> tokenId)
    val claims = buildGenericPayload(clientId, userId, expiry, customClaims)
    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }

  def createIdToken(clientId: String, userId: String, setId: String, tokenId: String, userData: Map[String, String], expiry: Long): String = {
    val header = JwtHeader(algorithm = JwtAlgorithm.RS256)
      .withKeyId(jwks.getKeyID)
      .toJson
    val customClaims = List("tsid" -> setId, "tid" -> tokenId) ++ userData
    val claims = buildGenericPayload(clientId, userId, expiry, customClaims)
    Jwt.encode(header, claims, jwks.toPrivateKey, JwtAlgorithm.RS256)
  }

  def createClientAccessToken(clientId: String, setId: String, tokenId: String, expiry: Long): String = {
    val customClaims = List("tsid" -> setId, "tid" -> tokenId)
    val claims = buildGenericPayload(clientId, clientId, expiry, customClaims)
    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }

  def createRefreshToken(clientId: String, userId: String, expiry: Long, setId: String, tokenId: String, scope: Seq[String]): String = {
    val now = Instant.now

    val token = RefreshToken(
      sub = userId,
      aud = clientId,
      iss = issuer,
      iat = now.getEpochSecond,
      exp = now.plusMillis(expiry).getEpochSecond,
      tsid = setId,
      tid = tokenId,
      scope
    )

    RefreshToken.enc(token)
  }

  def generateTokenRecordSetId: String = {
    UUID.randomUUID().toString
  }

  def createTokenRecordSet(recordSetId: String, userId: String, appId: String, accessId: String, idId: Option[String], refreshId: Option[String])
                          (implicit ec: ExC): Future[MongoCreateResponse] = {
    val record = TokenRecord(recordSetId, userId, appId, accessId, idId, refreshId, issuedAt = new DateTime())
    tokenRecordStore.createTokenRecord(record) map {
      case resp@MongoSuccessCreate =>
        logger.info(s"[createTokenRecordSet] - Created new token record for user $userId in context of app $appId under record set $recordSetId")
        resp
      case resp@MongoFailedCreate =>
        logger.error(s"[createTokenRecordSet] - Unable to create new token record set")
        resp
    }
  }

  def lookupTokenRecordSet(recordSetId: String, userId: String)(implicit ec: ExC): Future[Option[TokenRecord]] = {
    val query = and(
      equal("tokenSetId", recordSetId),
      equal("userId", userId)
    )
    tokenRecordStore.validateTokenRecord(query) map { recordSet =>
      if(recordSet.isDefined) {
        logger.info(s"[lookupTokenRecordSet] - Found token record set matching $recordSet")
      } else {
        logger.warn(s"[lookupTokenRecordSet] - Could not find token record set matching $recordSetId")
      }
      recordSet
    }
  }

  def updateTokenRecordSet(recordSetId: String, accessTokenId: String, idTokenId: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = equal("tokenSetId", recordSetId)
    val update = combine(
      set("accessTokenId", accessTokenId),
      set("idTokenId", idTokenId),
      set("issuedAt", new DateTime())
    )

    tokenRecordStore.updateTokenRecord(query, update)
  }

  def getActiveSessionsFor(userId: String, appId: String): Future[Seq[TokenRecord]] = {
    val query = and(equal("userId", userId), equal("appId", appId))
    tokenRecordStore.getActiveRecords(query)
  }

  def revokeTokens(tokenSetId: String, appId: String, userId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = and(equal("tokenSetId", tokenSetId), equal("appId", appId), equal("userId", userId))
    tokenRecordStore.deleteTokenRecord(query)
  }

  def extractTokenIds(token: String): (String, String) = {
    val json = Json.parse(Jwt.decode(token, signature, Seq(JwtAlgorithm.HS512)).get.toJson)
    json.\("tsid").as[String] -> json.\("tid").as[String]
  }

  def revokeSpecificToken(tokenType: Option[String], setId: String, tokenId: String)
                         (implicit ec: ExC): Future[Either[MongoUpdatedResponse, MongoDeleteResponse]] = {
    if(tokenType.isDefined) {
      val field = tokenType.collect {
        case "access_token"  => "accessTokenId"
        case "refresh_token" => "refreshTokenId"
      }

      val eqs = Seq(equal("tokenSetId", setId)) ++ field.map(field => Seq(equal(field, tokenId))).getOrElse(Seq())
      val query = and(eqs:_*)

      if(field.isDefined) {
        val update = set(field.get, "")
        tokenRecordStore.updateTokenRecord(query, update) map(Left(_))
      } else {
        tokenRecordStore.deleteTokenRecord(query) map(Right(_))
      }
    } else {
      val query = equal("tokenSetId", setId)
      tokenRecordStore.deleteTokenRecord(query) map(Right(_))
    }
  }

  def convertDaysMinsToMilli(tokenExpiry: TokenExpiry): (Long, Long, Long) = {
    val dayAndMinToMill: (Long, Long) => Long = (day, min) => {
      TimeUnit.DAYS.toMillis(day) + TimeUnit.MINUTES.toMillis(min)
    }

    (
      dayAndMinToMill(tokenExpiry.idTokenDays, tokenExpiry.idTokenMins),
      dayAndMinToMill(tokenExpiry.accessTokenDays, tokenExpiry.accessTokenMins),
      dayAndMinToMill(tokenExpiry.refreshTokenDays, tokenExpiry.refreshTokenMins)
    )
  }

  def convertMilliToTokenExpiry(idTokenExpiry: Long, accessTokenExpiry: Long, refreshTokenExpiry: Long): TokenExpiry = {
    val getDaysAndMins: Long => (Long, Long) = expiry => {
      val days = TimeUnit.MILLISECONDS.toDays(expiry)

      if(days < 1) {
        val mins = TimeUnit.MILLISECONDS.toMinutes(expiry)
        days -> mins
      } else {
        val daysInMillis = TimeUnit.DAYS.toMillis(days)
        val mins = TimeUnit.MILLISECONDS.toMinutes(expiry - daysInMillis)

        days -> mins
      }
    }

    val (idDays, idMins) = getDaysAndMins(idTokenExpiry)
    val (accessDays, accessMins) = getDaysAndMins(accessTokenExpiry)
    val (refreshDays, refreshMins) = getDaysAndMins(refreshTokenExpiry)

    TokenExpiry(
      idTokenDays = idDays,
      idTokenMins = idMins,
      accessTokenDays = accessDays,
      accessTokenMins = accessMins,
      refreshTokenDays = refreshDays,
      refreshTokenMins = refreshMins
    )
  }
}
