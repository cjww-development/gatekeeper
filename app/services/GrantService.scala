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

import java.util.UUID

import com.cjwwdev.mongo.responses.MongoCreateResponse
import database.{AppStore, GrantStore}
import javax.inject.Inject
import models.{AuthorisationRequest, Grant, RegisteredApplication, Scopes}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.Configuration

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultGrantService @Inject()(val appStore: AppStore,
                                    val grantStore: GrantStore,
                                    val config: Configuration) extends GrantService {
  override protected val scopes: Scopes = Scopes(
    reads = config.get[Seq[String]]("scopes.read").map(_.trim),
    writes = config.get[Seq[String]]("scopes.write").map(_.trim)
  )
}

trait GrantService {

  val appStore: AppStore
  val grantStore: GrantStore

  private val logger = LoggerFactory.getLogger(this.getClass)

  protected val scopes: Scopes

  def getRegisteredApp(clientId: String): Future[Option[RegisteredApplication]] = {
    appStore.validateAppOn("clientId", clientId)
  }

  def validateRedirectUrl(redirectUrl: String, savedRedirectUrl: String): Boolean = {
    val valid = redirectUrl.trim == savedRedirectUrl.trim
    logger.info(s"[validateRedirectUrl] - Are the provided redirect urls valid? $valid")
    valid
  }

  def validateRequestedScopes(inboundScopes: Seq[String]): Boolean = {
    val inboundReads  = inboundScopes.filter(_.startsWith("read:")).map(_.replace("read:", "").trim)
    val inboundWrites = inboundScopes.filter(_.startsWith("write:")).map(_.replace("write:", "").trim)

    val validatedReads  = inboundReads.map(scopes.reads.contains)
    val validatedWrites = inboundWrites.map(scopes.writes.contains)

    val valid = !(validatedReads.contains(false) || validatedWrites.contains(false))
    logger.info(s"[validateRequestedScopes] - Are the requested scopes valid? $valid")
    valid
  }

  def saveGrant(grant: Grant)(implicit ec: ExC): Future[MongoCreateResponse] = {
    grantStore.createGrant(grant)
  }

  def validateGrant(authCode: String)(implicit ec: ExC): Future[Option[Grant]] = {
    grantStore.validateGrant(authCode) map { grant =>
      if(grant.isDefined) {
        logger.info(s"[validateGrant] - Authorisation grant found")
      } else {
        logger.warn(s"[validateGrant] - No authorisation grant was found")
      }
      grant
    }
  }
}
