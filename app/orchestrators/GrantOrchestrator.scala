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
import models.{Grant, RegisteredApplication}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import services.{AccountService, GrantService, ScopeService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait GrantInitiateResponse
case object InvalidApplication extends GrantInitiateResponse
case object InvalidScopesRequested extends GrantInitiateResponse
case class ValidatedGrantRequest(app: RegisteredApplication, scopes: String) extends GrantInitiateResponse

class DefaultGrantOrchestrator @Inject()(val grantService: GrantService,
                                         val accountService: AccountService,
                                         val scopeService: ScopeService) extends GrantOrchestrator

trait GrantOrchestrator {

  protected val grantService: GrantService
  protected val accountService: AccountService
  protected val scopeService: ScopeService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def validateIncomingGrant(responseType: String, clientId: String, scope: String)(implicit ec: ExC): Future[GrantInitiateResponse] = {
    grantService.getRegisteredApp(clientId) flatMap {
      case Some(app) =>
        val validScopes = scopeService.validateScopes(scope)
        if(validScopes) {
          accountService.getOrganisationAccountInfo(app.owner) map { user =>
            ValidatedGrantRequest(
              app = app.copy(owner = user.getOrElse("userName", "")),
              scopes = scope
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
  }

  def saveIncomingGrant(responseType: String, clientId: String, userId: String, scope: Seq[String])(implicit ec: ExC): Future[Option[Grant]] = {
    accountService.determineAccountTypeFromId(userId) match {
      case Some(accType) => grantService.getRegisteredApp(clientId) flatMap {
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
          grantService.saveGrant(grant).map {
            case MongoSuccessCreate =>
              logger.info(s"[saveIncomingGrant] - Successfully created authorisation grant for userId $userId targeted for client $clientId")
              Some(grant)
            case MongoFailedCreate  =>
              logger.error(s"[saveIncomingGrant] - There was a problem saving the grant for userId $userId")
              None
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
