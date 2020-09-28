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

import com.cjwwdev.mongo.responses.{MongoDeleteResponse, MongoFailedDelete, MongoSuccessDelete}
import com.cjwwdev.security.deobfuscation.DeObfuscators
import com.cjwwdev.security.obfuscation.Obfuscators
import javax.inject.Inject
import models.{AuthorisedClient, RegisteredApplication}
import org.slf4j.{Logger, LoggerFactory}
import services._

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait AppUpdateResponse
case object SecretsUpdated extends AppUpdateResponse
case object NoAppFound extends AppUpdateResponse
case object UpdatedFailed extends AppUpdateResponse

class DefaultClientOrchestrator @Inject()(val clientService: ClientService,
                                          val userService: UserService) extends ClientOrchestrator {
  override val locale: String = ""
}

trait ClientOrchestrator extends Obfuscators with DeObfuscators {

  protected val clientService: ClientService
  protected val userService: UserService

  override val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def getRegisteredApp(orgUserId: String, appId: String)(implicit ec: ExC): Future[Option[RegisteredApplication]] = {
    clientService.getRegisteredApp(orgUserId, appId).map {
      _.map(app => app.copy(
        clientId = stringDeObfuscate.decrypt(app.clientId).getOrElse(throw new Exception(s"Could not decrypt clientId for app ${app.appId}")),
        clientSecret = app.clientSecret.map(sec => stringDeObfuscate.decrypt(sec).getOrElse(throw new Exception(s"Could not decrypt clientSecret for app ${app.appId}")))
      ))
    }
  }

  def getRegisteredApps(orgUserId: String, groupedBy: Int)(implicit ec: ExC): Future[Seq[Seq[RegisteredApplication]]] = {
    clientService.getRegisteredAppsFor(orgUserId) map {
      _.map(app => app.copy(
        clientId = stringDeObfuscate.decrypt(app.clientId).getOrElse(throw new Exception(s"Could not decrypt clientId for app ${app.appId}")),
        clientSecret = app.clientSecret.map(sec => stringDeObfuscate.decrypt(sec).getOrElse(throw new Exception(s"Could not decrypt clientSecret for app ${app.appId}")))
      ))
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

  def getAuthorisedApp(userId: String, appId: String)(implicit ec: ExC): Future[Option[(RegisteredApplication, AuthorisedClient)]] = {
    userService.getUserInfo(userId).flatMap {
      case Some(user) => if(user.authorisedClients.exists(_.appId == appId)) {
        clientService.getRegisteredApp(appId) flatMap {
          case Some(app) => userService.getUserInfo(app.owner) map { _user =>
            Some(app.copy(owner = _user.map(_.userName).getOrElse(app.owner)) -> user.authorisedClients.find(_.appId == appId).get)
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
}
