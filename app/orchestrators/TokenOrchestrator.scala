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
import services.{AccountService, GrantService, ScopeService, TokenService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait TokenResponse
case class Issued(token: String) extends TokenResponse
case object InvalidCodeOrState extends TokenResponse

class DefaultTokenOrchestrator @Inject()(val grantService: GrantService,
                                         val tokenService: TokenService) extends TokenOrchestrator

trait TokenOrchestrator {

  protected val grantService: GrantService
  protected val tokenService: TokenService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def issueToken(authCode: String)(implicit ec: ExC): Future[TokenResponse] = {
    grantService.validateGrant(authCode) map {
      case Some(grant) =>
        logger.info("[issueToken] - Grant has been validated")
        val token = tokenService.createAccessToken(grant.userId, grant.accType)
        Issued(token)
      case None        =>
        logger.warn("[issueToken] - Grant could not be validated")
        InvalidCodeOrState
    }
  }
}
