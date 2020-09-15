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
import services.{AccountService, ClientService, GrantService, ScopeService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait GrantInitiateResponse
case object InvalidApplication extends GrantInitiateResponse
case object InvalidScopesRequested extends GrantInitiateResponse
case object InvalidResponseType extends GrantInitiateResponse
case class ValidatedGrantRequest(app: RegisteredApplication, scopes: Seq[Scope]) extends GrantInitiateResponse
case object PreviouslyAuthorised extends GrantInitiateResponse

class DefaultGrantOrchestrator @Inject()(val grantService: GrantService,
                                         val clientService: ClientService,
                                         val accountService: AccountService,
                                         val scopeService: ScopeService) extends GrantOrchestrator

trait GrantOrchestrator {

  protected val grantService: GrantService
  protected val clientService: ClientService
  protected val accountService: AccountService
  protected val scopeService: ScopeService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateIncomingGrant(responseType: String, clientId: String, scope: String, requestingUserId: String)(implicit ec: ExC): Future[GrantInitiateResponse] = {
    if(responseType == "code") {
      clientService.getRegisteredAppById(clientId) flatMap {
        case Some(app) =>
          val validScopes = scopeService.validateScopes(scope)
          if(validScopes) {
            for {
              Some(orgUser) <- accountService.getOrganisationAccountInfo(app.owner)
              Some(reqUser) <- requestingUserId match {
                case id if id.startsWith("user-") => accountService.getIndividualAccountInfo(requestingUserId)
                case id if id.startsWith("org-user-") => accountService.getOrganisationAccountInfo(requestingUserId)
              }
            } yield if(reqUser.authorisedClients.contains(app.appId)) {
              PreviouslyAuthorised
            } else {
              ValidatedGrantRequest(
                app = app.copy(owner = orgUser.userName),
                scopes = {
                  val splitScopes = scope.split(",").map(_.trim)
                  scopeService.getValidScopes.filter(scp => splitScopes.contains(scp.name))
                }
              )
            }
          } else {
            logger.warn(s"[validateIncomingGrant] - The requested scopes weren't valid")
            Future.successful(InvalidScopesRequested)
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

  def saveIncomingGrant(responseType: String, clientId: String, userId: String, scope: Seq[String])(implicit ec: ExC): Future[Option[Grant]] = {
    accountService.determineAccountTypeFromId(userId) match {
      case Some(accType) => clientService.getRegisteredAppById(clientId) flatMap {
        case Some(app) =>
          val grant = Grant(
            responseType,
            UUID.randomUUID().toString,
            scope,
            clientId,
            userId,
            accType,
            app.redirectUrl,
            DateTime.now()
          )
          grantService.saveGrant(grant).flatMap {
            case MongoSuccessCreate =>
              logger.info(s"[saveIncomingGrant] - Successfully created authorisation grant for userId $userId targeted for client $clientId")
              accountService.linkAuthorisedClientTo(userId, app.appId) map {
                _ => Some(grant)
              }
            case MongoFailedCreate  =>
              logger.error(s"[saveIncomingGrant] - There was a problem saving the grant for userId $userId")
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

//  def createErrorResponse()
}
