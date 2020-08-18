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
import org.slf4j.LoggerFactory
import services.{AccountService, GrantService, TokenService}

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait TokenResponse
case class Issued(tokenType: String, scope: String, expiresIn: Long, accessToken: String, idToken: String) extends TokenResponse
case object InvalidGrant extends TokenResponse
case object InvalidGrantType extends TokenResponse
case object InvalidUser extends TokenResponse

class DefaultTokenOrchestrator @Inject()(val grantService: GrantService,
                                         val accountService: AccountService,
                                         val tokenService: TokenService) extends TokenOrchestrator

trait TokenOrchestrator {

  protected val grantService: GrantService
  protected val tokenService: TokenService
  protected val accountService: AccountService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def issueToken(grantType: String, authCode: String, clientId: String, redirectUri: String)(implicit ec: ExC): Future[TokenResponse] = {
    grantType match {
      case "authorization_code" =>
        logger.info(s"[issueToken] - Issuing tokens using the $grantType grant")
        authorizationCodeGrant(authCode, clientId, redirectUri)
      case e =>
        logger.error(s"[issueToken] - Could not validate grant type $e")
        Future.successful(InvalidGrantType)
    }
  }

  private def authorizationCodeGrant(authCode: String, clientId: String, redirectUri: String)(implicit ec: ExC): Future[TokenResponse] = {
    grantService.validateGrant(authCode, clientId, redirectUri) flatMap {
      case Some(grant) =>
        logger.info("[authorizationCodeGrant] - Grant has been validated")
        val user = grant.accType match {
          case "individual"   => accountService.getIndividualAccountInfo(grant.userId)
          case "organisation" => accountService.getOrganisationAccountInfo(grant.userId)
        }

        user.map { userData =>
          if(userData.nonEmpty) {
            logger.info(s"[authorizationCodeGrant] - Issuing new Id and access token for ${grant.userId} for use by clientId ${grant.clientId}")
            Issued(
              tokenType = "Bearer",
              scope = grant.scope.mkString(","),
              expiresIn = tokenService.expiry,
              accessToken = tokenService.createAccessToken(grant.clientId, grant.userId, grant.scope.mkString(",")),
              idToken = tokenService.createIdToken(grant.clientId, grant.userId, userData, grant.accType)
            )
          } else {
            logger.warn(s"[authorizationCodeGrant] - could not validate user on userId ${grant.userId}")
            InvalidUser
          }
        }
      case None =>
        logger.warn(s"[authorizationCodeGrant] - Couldn't validate grant")
        Future.successful(InvalidGrant)
    }
  }


}
