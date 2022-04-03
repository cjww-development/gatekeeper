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

import com.typesafe.config.Config
import database.AppStore
import dev.cjww.mongo.responses.{MongoDeleteResponse, MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import models.{PresetService, RegisteredApplication}
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import utils.StringUtils._

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.util.Try

sealed trait RegenerationResponse
case object RegeneratedId extends RegenerationResponse
case object RegeneratedIdAndSecret extends RegenerationResponse
case object RegenerationFailed extends RegenerationResponse

class DefaultClientService @Inject()(val appStore: AppStore,
                                     val config: Configuration) extends ClientService {
  override protected val presetServices: Seq[PresetService] = {
    config.get[Seq[Config]]("well-known-services").map { conf =>
      PresetService(
        name = conf.getString("name"),
        desc = conf.getString("desc"),
        icon = conf.getString("icon"),
        domain = Try(conf.getString("domain")).map(Some(_)).getOrElse(None),
        redirect = conf.getString("redirect")
      )
    }
  }
}

trait ClientService {
  val appStore: AppStore

  protected val presetServices: Seq[PresetService]

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getPresetServices: Seq[PresetService] = presetServices

  def getRegisteredApp(orgUserId: String, appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = and(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    appStore.validateAppOn(query) map { app =>
      if(app.isDefined) {
        logger.info(s"[getRegisteredApp] - Found app $appId belonging to $orgUserId")
      } else {
        logger.warn(s"[getRegisteredApp] - No app found app $appId belonging to $orgUserId")
      }
      app
    }
  }

  def getRegisteredApp(appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = and(equal("appId", appId))

    appStore.validateAppOn(query) map { app =>
      if(app.isDefined) {
        logger.info(s"[getRegisteredApp] - Found app $appId")
      } else {
        logger.warn(s"[getRegisteredApp] - No app found app $appId")
      }
      app
    }
  }

  def getRegisteredAppByIdAndSecret(clientId: String, clientSecret: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = and(
      equal("clientId", clientId.encrypt),
      equal("clientSecret", clientSecret.encrypt)
    )

    appStore.validateAppOn(query) map { app =>
      if(app.isDefined) {
        logger.info(s"[getRegisteredAppByIdAndSecret] - Found app ${app.get.appId} belonging to ${app.get.owner}")
      } else {
        logger.warn(s"[getRegisteredAppByIdAndSecret] - No app found matching the given clientId and clientSecret")
      }
      app
    }
  }

  def getRegisteredAppById(clientId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    val query = and(
      equal("clientId", clientId.encrypt)
    )

    appStore.validateAppOn(query) map { app =>
      if(app.isDefined) {
        logger.info(s"[getRegisteredAppById] - Found app ${app.get.appId} belonging to ${app.get.owner}")
      } else {
        logger.warn(s"[getRegisteredAppById] - No app found matching the given clientId")
      }
      app
    }
  }

  def getRegisteredAppsFor(orgUserId: String)(implicit ec: ExC): Future[Seq[RegisteredApplication]] = {
    appStore.getAppsOwnedBy(orgUserId) map { apps =>
      if(apps.nonEmpty) {
        logger.info(s"[getRegisteredAppsFor] - Found ${apps.length} apps $orgUserId")
      } else {
        logger.warn(s"[getRegisteredAppsFor] - No apps found belonging to $orgUserId")
      }
      apps
    }
  }

  def regenerateClientIdAndSecret(orgUserId: String, appId: String, isConfidential: Boolean)(implicit ec: ExC): Future[RegenerationResponse] = {
    val clientId = RegisteredApplication.generateIds(iterations = 1)
    val clientSecret = RegisteredApplication.generateIds(iterations = 2)
    val query = and(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    val update = if(isConfidential) {
      combine(set("clientId", clientId), set("clientSecret", clientSecret))
    } else {
      combine(set("clientId", clientId))
    }

    appStore.updateApp(query, update) map {
      case MongoSuccessUpdate =>
        logger.info(s"[regenerateClientIdAndSecret] - Regenerated clientId ${if(isConfidential) "and clientSecret"} for appId $appId")
        if(isConfidential) RegeneratedIdAndSecret else RegeneratedId
      case MongoFailedUpdate =>
        logger.warn(s"[regenerateClientIdAndSecret] - There was a problem regenerating Ids and or secrets for appId $appId")
        RegenerationFailed
    }
  }

  def deleteClient(orgUserId: String, appId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = combine(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    appStore.deleteApp(query)
  }

  def updateOAuth2Flows(flows: Seq[String], appId: String, orgUserId: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = combine(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    val update = set("oauth2Flows", flows)

    appStore.updateApp(query, update)
  }

  def updateOAuth2Scopes(scopes: Seq[String], appId: String, orgUserId: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = combine(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    val update = set("oauth2Scopes", scopes)

    appStore.updateApp(query, update)
  }

  def updateTokenExpiry(orgUserId: String, appId: String, idExpiry: Long, accessExpiry: Long, refreshExpiry: Long)
                       (implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = and(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    val update = combine(
      set("idTokenExpiry", idExpiry),
      set("accessTokenExpiry", accessExpiry),
      set("refreshTokenExpiry", refreshExpiry)
    )

    appStore.updateApp(query, update)
  }

  def updateRedirects(orgUserId: String, appId: String, homeUrl: String, redirectUrl: String)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = and(
      equal("owner", orgUserId),
      equal("appId", appId)
    )

    val update = combine(
      set("homeUrl", homeUrl),
      set("redirectUrl", redirectUrl)
    )

    appStore.updateApp(query, update)
  }

  def updateBasicDetails(owner: String, appId: String, name: String, desc: String, iconUrl: Option[String])(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    val query = and(
      equal("owner", owner),
      equal("appId", appId)
    )

    val update = if(iconUrl.nonEmpty) {
      combine(set("name", name), set("desc", desc), set("iconUrl", iconUrl.get))
    } else {
      combine(set("name", name), set("desc", desc), unset("iconUrl"))
    }

    appStore.updateApp(query, update)
  }
}
