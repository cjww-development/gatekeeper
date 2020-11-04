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

import java.security.MessageDigest
import java.util.Base64

import com.cjwwdev.mongo.responses.MongoCreateResponse
import database.{AppStore, GrantStore}
import javax.inject.Inject
import models.Grant
import org.mongodb.scala.model.Filters.{and, equal}
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultGrantService @Inject()(val appStore: AppStore,
                                    val grantStore: GrantStore) extends GrantService

trait GrantService {

  val appStore: AppStore
  val grantStore: GrantStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateRedirectUrl(redirectUrl: String, savedRedirectUrl: String): Boolean = {
    val valid = redirectUrl.trim == savedRedirectUrl.trim
    logger.info(s"[validateRedirectUrl] - Are the provided redirect urls valid? $valid")
    valid
  }

  def saveGrant(grant: Grant)(implicit ec: ExC): Future[MongoCreateResponse] = {
    grantStore.createGrant(grant)
  }

  def validateGrant(authCode: String, clientId: String, redirectUri: String, codeVerifier: Option[String])(implicit ec: ExC): Future[Option[Grant]] = {
    def transformVeriferToChallenge(codeVerifier: String): String = {
      val bytes = codeVerifier.getBytes("US-ASCII")
      val messageDigest = MessageDigest.getInstance("SHA-256")
      messageDigest.update(bytes, 0, bytes.length)
      val digest = messageDigest.digest
      Base64
        .getUrlEncoder
        .withoutPadding
        .encodeToString(digest)
    }

    val query = codeVerifier.fold(
      and(
        equal("authCode", authCode),
        equal("clientId", clientId),
        equal("redirectUri", redirectUri)
      )
    )(verifier => and(
      equal("authCode", authCode),
      equal("clientId", clientId),
      equal("redirectUri", redirectUri),
      equal("codeVerifier", verifier)
    ))

    grantStore.validateGrant(query) map { grant =>
      if(grant.isDefined) {
        logger.info(s"[validateGrant] - Authorisation grant found")
        (codeVerifier, grant.get.codeChallenge) match {
          case (Some(verifier), Some(challenge)) => if(transformVeriferToChallenge(verifier) == challenge) {
            logger.info(s"[validateGrant] - Validated grant using the PKCE verifier")
            grant
          } else {
            logger.warn(s"[validateGrant] - Grant found but unable to validate using the supplied verifier")
            None
          }
          case (None, Some(_)) =>
            logger.warn("[validateGrant] - Grant found containing PKCE challenge but no verifier was supplied")
            None
          case (Some(_), None) =>
            logger.warn(s"[validateGrant] - Grant found containing no PKCE challenge but a verifier was supplied; retry with basic auth")
            None
          case (None, None) =>
            logger.info(s"[validateGrant] - Grant found with no PKCE challenge and no verifier was supplied")
            grant
        }
      } else {
        logger.warn(s"[validateGrant] - No authorisation grant was found")
        None
      }
    }
  }
}
