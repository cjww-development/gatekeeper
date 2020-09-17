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

import com.cjwwdev.security.deobfuscation.DeObfuscators
import javax.inject.Inject
import org.slf4j.LoggerFactory
import play.api.mvc.Request
import services.{AccountService, ClientService, GrantService, ScopeService, TokenService}
import utils.BasicAuth

import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait TokenResponse
case class Issued(tokenType: String, scope: String, expiresIn: Long, accessToken: String, idToken: Option[String]) extends TokenResponse
case object InvalidGrant extends TokenResponse
case object InvalidGrantType extends TokenResponse
case object InvalidUser extends TokenResponse
case object InvalidClient extends TokenResponse

class DefaultTokenOrchestrator @Inject()(val grantService: GrantService,
                                         val accountService: AccountService,
                                         val clientService: ClientService,
                                         val scopeService: ScopeService,
                                         val tokenService: TokenService) extends TokenOrchestrator {
  override protected val basicAuth: BasicAuth = BasicAuth
}

trait TokenOrchestrator extends DeObfuscators {

  override val locale: String = ""

  protected val grantService: GrantService
  protected val tokenService: TokenService
  protected val accountService: AccountService
  protected val clientService: ClientService
  protected val scopeService: ScopeService
  protected val basicAuth: BasicAuth

  override val logger = LoggerFactory.getLogger(this.getClass)

  def authorizationCodeGrant(authCode: String, clientId: String, redirectUri: String)(implicit ec: ExC): Future[TokenResponse] = {
    grantService.validateGrant(authCode, clientId, redirectUri) flatMap {
      case Some(grant) =>
        logger.info("[authorizationCodeGrant] - Grant has been validated")
        val user = grant.accType match {
          case "individual"   => accountService.getIndividualAccountInfo(grant.userId)
          case "organisation" => accountService.getOrganisationAccountInfo(grant.userId)
        }

        val scopes = scopeService.getScopeDetails(grant.scope)

        user.map {
          case Some(userData) =>
            logger.info(s"[authorizationCodeGrant] - Issuing new Id and access token for ${grant.userId} for use by clientId ${grant.clientId}")
            val scopedData = userData.toMap.filter{ case (k, _) =>  scopes.exists(_.name == k)}
            Issued(
              tokenType = "Bearer",
              scope = grant.scope.mkString(","),
              expiresIn = tokenService.expiry,
              accessToken = tokenService.createAccessToken(grant.clientId, grant.userId, grant.scope.mkString(",")),
              idToken = if(scopes.exists(_.name == "openid")) {
                Some(tokenService.createIdToken(grant.clientId, grant.userId, scopedData))
              } else {
                None
              }
            )
          case None =>
            logger.warn(s"[authorizationCodeGrant] - could not validate user on userId ${grant.userId}")
            InvalidUser
        }
      case None =>
        logger.warn(s"[authorizationCodeGrant] - Couldn't validate grant")
        Future.successful(InvalidGrant)
    }
  }

  def clientCredentialsGrant(scope: String)(implicit ec: ExC, req: Request[_]): Future[TokenResponse] = {
    basicAuth.decode.fold({
      case (clientId, clientSecret) => clientService.getRegisteredAppByIdAndSecret(clientId, clientSecret) map {
        case Some(app) =>
          logger.info(s"[clientCredentialsGrant] - Issuing new access token for client ${app.appId} for use by client ${app.appId}")
          Issued(
            tokenType = "Bearer",
            scope,
            expiresIn = tokenService.expiry,
            accessToken = tokenService
              .createClientAccessToken(stringDeObfuscate.decrypt(app.clientId)
              .getOrElse(throw new Exception(s"Could not decrypt clientId for client ${app.appId}"))),
            idToken = None
          )
        case None      =>
          logger.warn(s"[clientCredentialsGrant] - No matching client was found, unable to issue client token")
          InvalidClient
      }
    },{
      _ =>
        logger.warn(s"[clientCredentialsGrant] - Could not validate the requested client")
        Future.successful(InvalidClient)
    })
  }
}
