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

import java.util.UUID

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import javax.inject.Inject
import models.{Grant, RegisteredApplication, Scope}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import services.{ClientService, GrantService, ScopeService, UserService}

import scala.concurrent.{Future, blocking, ExecutionContext => ExC}

sealed trait GrantInitiateResponse
case object InvalidApplication extends GrantInitiateResponse
case object InvalidScopesRequested extends GrantInitiateResponse
case object InvalidOAuth2Flow extends GrantInitiateResponse
case object InvalidResponseType extends GrantInitiateResponse
case class ValidatedGrantRequest(app: RegisteredApplication, scopes: Seq[Scope]) extends GrantInitiateResponse
case class ScopeDrift(app: RegisteredApplication, authorisedScopes: Seq[Scope], requestedScopes: Seq[Scope]) extends GrantInitiateResponse
case object PreviouslyAuthorised extends GrantInitiateResponse

class DefaultGrantOrchestrator @Inject()(val grantService: GrantService,
                                         val clientService: ClientService,
                                         val userService: UserService,
                                         val scopeService: ScopeService) extends GrantOrchestrator

trait GrantOrchestrator {

  protected val grantService: GrantService
  protected val clientService: ClientService
  protected val userService: UserService
  protected val scopeService: ScopeService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateIncomingGrant(responseType: String, clientId: String, scope: String, requestingUserId: String)(implicit ec: ExC): Future[GrantInitiateResponse] = {
    if(responseType == "code") {
      clientService.getRegisteredAppById(clientId) flatMap {
        case Some(app) => if(app.oauth2Flows.contains("authorization_code")) {
          val validScopes = scopeService.validateScopes(scope, app.oauth2Scopes.toSeq)
          if(validScopes) {
            for {
              Some(orgUser) <- userService.getUserInfo(app.owner)
              Some(reqUser) <- userService.getUserInfo(requestingUserId)
            } yield if(reqUser.authorisedClients.exists(_.appId == app.appId)) {
              val requestedScopes = scope.split(" ").map(_.trim).toSeq
              val authorisedScopes = reqUser.authorisedClients.find(_.appId == app.appId).get.authorisedScopes
              if(authorisedScopes != requestedScopes) {
                ScopeDrift(
                  app = app.copy(owner = orgUser.userName),
                  scopeService.getScopeDetails(authorisedScopes),
                  scopeService.getScopeDetails(requestedScopes)
                )
              } else {
                PreviouslyAuthorised
              }
            } else {
              ValidatedGrantRequest(
                app = app.copy(owner = orgUser.userName),
                scopes = {
                  val splitScopes = scope.split(" ").map(_.trim)
                  scopeService.getValidScopes.filter(scp => splitScopes.contains(scp.name))
                }
              )
            }
          } else {
            logger.warn(s"[validateIncomingGrant] - The requested scopes weren't valid")
            Future.successful(InvalidScopesRequested)
          }
        } else {
          logger.warn(s"[validateIncomingGrant] - The authorization_code OAuth2 flow is not supported by client $clientId")
          Future.successful(InvalidOAuth2Flow)
        }
        case None =>
          logger.warn(s"[validateIncomingGrant] - There are no clients registered against clientId $clientId")
          Future.successful(InvalidApplication)
      }
    } else {
      logger.warn(s"[validateIncomingGrant] - An invalid response type was found (${responseType})")
      Future.successful(InvalidResponseType)
    }
  }

  def saveIncomingGrant(responseType: String, clientId: String, userId: String, scope: Seq[String], codeVerifier: Option[String], codeChallenge: Option[String], codeChallengeMethod: Option[String])(implicit ec: ExC): Future[Option[Grant]] = {
    userService.getUserInfo(userId) flatMap {
      case Some(user) => clientService.getRegisteredAppById(clientId) flatMap {
        case Some(app) =>
          val grant = Grant(
            responseType,
            UUID.randomUUID().toString,
            scope,
            clientId,
            userId,
            user.accType,
            app.redirectUrl,
            codeVerifier,
            codeChallenge,
            codeChallengeMethod,
            DateTime.now()
          )
          grantService.saveGrant(grant).flatMap {
            case MongoSuccessCreate =>
              logger.info(s"[saveIncomingGrant] - Successfully created authorisation grant for userId $userId targeted for client $clientId")
              userService.linkClientToUser(userId, app.appId, grant.scope) map {
                _ => Some(grant)
              }
            case MongoFailedCreate  =>
              logger.error(s"[savLIncomingGrant] - There was a problem saving the grant for userId $userId")
              Future.successful(None)
          }
        case None =>
          logger.warn(s"[saveIncomingGrant] - No matching client found for clientId $clientId")
          Future.successful(None)
      }
      case None =>
        logger.warn(s"[saveIncomingGrant] - Unable to determine the users account type on userId $userId")
        Future.successful(None)
    }
  }
}
