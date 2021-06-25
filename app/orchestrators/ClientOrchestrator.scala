/*
 * Copyright 2021 CJWW Development
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

import dev.cjww.mongo.responses._
import utils.StringUtils._
import models.{AuthorisedClient, RegisteredApplication, TokenExpiry, TokenRecord}
import org.slf4j.{Logger, LoggerFactory}
import services.oauth2.{ClientService, RegeneratedId, RegeneratedIdAndSecret, RegenerationFailed, TokenService}
import services.users.{LinkResponse, UserService}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait AppUpdateResponse
case object SecretsUpdated extends AppUpdateResponse
case object NoAppFound extends AppUpdateResponse
case object UpdatedFailed extends AppUpdateResponse
case object FlowsUpdated extends AppUpdateResponse
case object ExpiryUpdated extends AppUpdateResponse
case object UrlsUpdated extends AppUpdateResponse

class DefaultClientOrchestrator @Inject()(val clientService: ClientService,
                                          val userService: UserService,
                                          val tokenService: TokenService) extends ClientOrchestrator

trait ClientOrchestrator {

  protected val clientService: ClientService
  protected val userService: UserService
  protected val tokenService: TokenService

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getRegisteredApp(orgUserId: String, appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    clientService.getRegisteredApp(orgUserId, appId).map {
      _.map(RegisteredApplication.decode)
    }
  }

  def getRegisteredApps(orgUserId: String, groupedBy: Int)(implicit ec: ExC): Future[Seq[Seq[RegisteredApplication]]] = {
    clientService.getRegisteredAppsFor(orgUserId) map {
      _.map(RegisteredApplication.decode)
      .grouped(groupedBy)
      .toSeq
    }
  }

  def regenerateClientIdAndSecret(orgUserId: String, appId: String)(implicit ec: ExC): Future[AppUpdateResponse] = {
    clientService.getRegisteredApp(orgUserId, appId) flatMap {
      case Some(app) =>
        clientService.regenerateClientIdAndSecret(app.owner, app.appId, app.clientType == "confidential") map {
          case RegeneratedId | RegeneratedIdAndSecret => SecretsUpdated
          case RegenerationFailed => UpdatedFailed
        }
      case None =>
        logger.warn(s"[regenerateClientIdAndSecret] - There was no matching app found for appId $appId owned by $orgUserId")
        Future.successful(NoAppFound)
    }
  }

  def deleteClient(orgUserId: String, appId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    clientService.deleteClient(orgUserId, appId) map {
      case resp@MongoSuccessDelete =>
        logger.info(s"[deleteClient] - Deleted the app $appId owned by $orgUserId")
        resp
      case resp@MongoFailedDelete =>
        logger.warn(s"[deleteClient] - There was a problem deleting the app $appId owned by $orgUserId")
        resp
    }
  }

  def getAuthorisedApps(userId: String)(implicit ec: ExC): Future[List[RegisteredApplication]] = {
    userService.getUserInfo(userId).flatMap {
      case Some(user) => Future
        .sequence(user.authorisedClients.map(app => clientService.getRegisteredApp(app.appId)))
        .map(_.flatten)
      case None => Future.successful(List.empty[RegisteredApplication])
    }
  }

  def getAuthorisedApp(userId: String, appId: String)(implicit ec: ExC): Future[Option[(RegisteredApplication, AuthorisedClient, Seq[TokenRecord])]] = {
    userService.getUserInfo(userId).flatMap {
      case Some(user) => if(user.authorisedClients.exists(_.appId == appId)) {
        clientService.getRegisteredApp(appId) flatMap {
          case Some(app) => for {
            _user <- userService.getUserInfo(app.owner)
            sessions <- tokenService.getActiveSessionsFor(userId, appId)
          } yield {
            val appWithOwner = app.copy(owner = _user.map(_.userName).getOrElse(app.owner))
            val authorisedClient = user.authorisedClients.find(_.appId == appId).get
            Some(appWithOwner, authorisedClient, sessions)
          }
          case None =>
            logger.warn(s"[getAuthorisedApp] - User found but no application was found")
            Future.successful(None)
        }
      } else {
        logger.warn(s"[getAuthorisedApp] - The application has not been previously authorised by the user, blocking request")
        Future.successful(None)
      }
      case None =>
        logger.warn(s"[getAuthorisedApp] - No user found whilst getting a users authorised app")
        Future.successful(None)
    }
  }

  def unlinkAppFromUser(appId: String, userId: String)(implicit ec: ExC): Future[LinkResponse] = {
    userService.unlinkClientFromUser(userId, appId)
  }

  def updateAppOAuthFlows(flows: Seq[String], appId: String, orgUserId: String)(implicit ec: ExC): Future[AppUpdateResponse] = {
    clientService.updateOAuth2Flows(flows, appId, orgUserId) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateAppOAuthFlows] - Updated the compatible oauth2 flows for app $appId belonging to org user $orgUserId")
        FlowsUpdated
      case MongoFailedUpdate =>
        logger.warn(s"[updateAppOAuthFlows] - Failed to update the compatible oauth2 flows for app $appId belonging to org user $orgUserId")
        UpdatedFailed
    }
  }

  def updateAppOAuthScopes(scopes: Seq[String], appId: String, orgUserId: String)(implicit ec: ExC): Future[AppUpdateResponse] = {
    clientService.updateOAuth2Scopes(scopes, appId, orgUserId) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateAppOAuthScopes] - Updated the compatible oauth2 scopes for app $appId belonging to org user $orgUserId")
        FlowsUpdated
      case MongoFailedUpdate =>
        logger.warn(s"[updateAppOAuthScopes] - Failed to update the compatible scopes flows for app $appId belonging to org user $orgUserId")
        UpdatedFailed
    }
  }

  def updateTokenExpiry(appId: String, orgUserId: String, tokenExpiry: TokenExpiry)(implicit ec: ExC): Future[AppUpdateResponse] = {
    val (id, access, refresh) = tokenService.convertDaysMinsToMilli(tokenExpiry)
    clientService.updateTokenExpiry(orgUserId, appId, id, access, refresh) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateTokenExpiry] - Updated the token expiry for app $appId belonging to org user $orgUserId")
        ExpiryUpdated
      case MongoFailedUpdate =>
        logger.warn(s"[updateTokenExpiry] - Failed to update the token expiry for app $appId belonging to org user $orgUserId")
        UpdatedFailed
    }
  }

  def getTokenExpiry(appId: String, orgUserId: String)(implicit ec: ExC): Future[Option[TokenExpiry]] = {
    clientService.getRegisteredApp(orgUserId, appId) map {
      case Some(app) =>
        logger.info(s"[getTokenExpiry] - Found registered app for appId $appId belonging to org user $orgUserId")
        Some(tokenService.convertMilliToTokenExpiry(app.idTokenExpiry, app.accessTokenExpiry, app.refreshTokenExpiry))
      case None =>
        logger.warn(s"[getTokenExpiry] - Could not find registered app for appId $appId belonging to org user $orgUserId")
        None
    }
  }

  def updateRedirects(appId: String, orgUserId: String, homeUrl: String, redirectUrl: String)(implicit ec: ExC): Future[AppUpdateResponse] = {
    clientService.updateRedirects(orgUserId, appId, homeUrl, redirectUrl) map {
      case MongoSuccessUpdate =>
        logger.info(s"[updateRedirects] - Updated the home url and redirect url for app $appId belonging to org user $orgUserId")
        UrlsUpdated
      case MongoFailedUpdate =>
        logger.warn(s"[updateRedirects] - Failed to update home url and redirect url for app $appId belonging to org user $orgUserId")
        UpdatedFailed
    }
  }
}
