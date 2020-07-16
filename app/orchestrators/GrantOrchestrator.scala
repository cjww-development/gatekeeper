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

import javax.inject.Inject
import models.{AuthorisationRequest, Grant, RegisteredApplication, Scopes}
import org.slf4j.LoggerFactory
import services.{AccountService, GrantService, ScopeService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait GrantInitiateResponse
case object InvalidApplication extends GrantInitiateResponse
case object InvalidScopesRequested extends GrantInitiateResponse
case object InvalidRedirectUrl extends GrantInitiateResponse
case object InvalidScopesAndRedirect extends GrantInitiateResponse
case class ValidatedGrantRequest(app: RegisteredApplication, scopes: Scopes) extends GrantInitiateResponse

class DefaultGrantOrchestrator @Inject()(val grantService: GrantService,
                                         val accountService: AccountService,
                                         val scopeService: ScopeService) extends GrantOrchestrator

trait GrantOrchestrator {

  protected val grantService: GrantService
  protected val accountService: AccountService
  protected val scopeService: ScopeService

  private val logger = LoggerFactory.getLogger(this.getClass)

  

  def initiateGrantRequest(authReq: AuthorisationRequest)(implicit ec: ExC): Future[GrantInitiateResponse] = {
    logger.info(s"current scope = ${authReq.scope}")
    grantService.getRegisteredApp(authReq.clientId) flatMap {
      case Some(app) =>
        val validation = grantService.validateRedirectUrl(authReq.redirectUri, app.redirectUrl) ->
          grantService.validateRequestedScopes(authReq.scope)

        validation match {
          case (true, true)   =>
            logger.info(s"[initiateGrantRequest] - Grant request validated")
            accountService.getOrganisationAccountInfo(app.owner) map {
              user => ValidatedGrantRequest(
                app.copy(owner = user.getOrElse("userName", "")),
                scopeService.makeScopesFromQuery(authReq.scope)
              )
            }
          case (true, false)  =>
            logger.warn(s"[initiateGrantRequest] - The requested scopes weren't valid")
            Future.successful(InvalidScopesRequested)
          case (false, true)  =>
            logger.warn(s"[initiateGrantRequest] - The requested redirectUrl wasn't valid")
            Future.successful(InvalidRedirectUrl)
          case (false, false) =>
            logger.warn(s"[initiateGrantRequest] - Neither the scopes or redirect was valid")
            Future.successful(InvalidScopesAndRedirect)
        }
      case None      =>
        logger.warn("[initiateGrantRequest] - The service doesn't exist")
        Future.successful(InvalidApplication)
    }
  }

  def saveGrant(authReq: AuthorisationRequest, accountId: String)(implicit ec: ExC): Future[Grant] = {
    accountService.determineAccountTypeFromId(accountId) match {
      case Right(accType) =>
        val grant = grantService.buildGrant(authReq, accountId, accType)
        grantService.saveGrant(grant) map(_ => grant)
    }
  }
}
