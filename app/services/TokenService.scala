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
import java.util.UUID

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoSuccessCreate}
import database.TokenRecordStore
import javax.inject.Inject
import models.{TokenRecord, UserInfo}
import org.mongodb.scala.model.Filters.{and, equal}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultTokenService @Inject()(val config: Configuration,
                                    val tokenRecordStore: TokenRecordStore) extends TokenService {
  override val issuer: String    = config.get[String]("jwt.iss")
  override val expiry: Long      = config.get[Long]("jwt.expiry")
  override val signature: String = config.get[String]("jwt.signature")
}

trait TokenService {

  val tokenRecordStore: TokenRecordStore

  val issuer: String
  val expiry: Long
  val signature: String

  private implicit val clock: Clock = Clock.systemUTC

  private val logger = LoggerFactory.getLogger(this.getClass)

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

  def createIdToken(clientId: String, userId: String, userData: Map[String, String]): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by(issuer)
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusSeconds(expiry).getEpochSecond)
      .about(userId)
      .++[String](userData.toSeq:_*)
      .toJson

    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }

  def createClientAccessToken(clientId: String): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by(issuer)
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusSeconds(expiry).getEpochSecond)
      .about(clientId)
      .toJson

    Jwt.encode(claims, signature, JwtAlgorithm.HS512)
  }

  def generateTokenRecordSetId: String = {
    UUID.randomUUID().toString
  }

  def createTokenRecordSet(recordSetId: String, userId: String, appId: String)(implicit ec: ExC): Future[MongoCreateResponse] = {
    val record = TokenRecord(recordSetId, userId, appId, issuedAt = new DateTime())
    tokenRecordStore.createTokenRecord(record) map {
      case resp@MongoSuccessCreate =>
        logger.info(s"[createTokenRecordSet] - Created new token record for user $userId in context of app $appId under record set $recordSetId")
        resp
      case resp@MongoFailedCreate =>
        logger.error(s"[createTokenRecordSet] - Unable to create new token record set")
        resp
    }
  }

  def lookupTokenRecordSet(recordSetId: String, userId: String, appId: String)(implicit ec: ExC): Future[Option[TokenRecord]] = {
    val query = and(equal("tokenSetId", recordSetId), equal("userId", userId), equal("appId", appId))
    tokenRecordStore.validateTokenRecord(query) map { recordSet =>
      if(recordSet.isDefined) {
        logger.info(s"[lookupTokenRecordSet] - Found token record set matching $recordSet")
      } else {
        logger.warn(s"[lookupTokenRecordSet] - Could not find token record set matching $recordSetId")
      }
      recordSet
    }
  }
}
