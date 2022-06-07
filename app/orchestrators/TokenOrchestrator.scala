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

package orchestrators

import dev.cjww.mongo.responses._
import models.{RefreshToken, RegisteredApplication}
import org.slf4j.{Logger, LoggerFactory}
import services.oauth2.{ClientService, GrantService, ScopeService, TokenService}
import services.users.UserService
import utils.StringUtils._

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait TokenResponse
case class Issued(tokenType: String,
                  scope: String,
                  expiresIn: Long,
                  accessToken: String,
                  idToken: Option[String],
                  refreshToken: Option[String]) extends TokenResponse
case object TokenError extends TokenResponse
case object InvalidGrant extends TokenResponse
case object InvalidGrantType extends TokenResponse
case object InvalidUser extends TokenResponse
case object InvalidClient extends TokenResponse
case object InvalidOAuthFlow extends TokenResponse
case object TokenClientMismatch extends TokenResponse

class DefaultTokenOrchestrator @Inject()(val grantService: GrantService,
                                         val userService: UserService,
                                         val clientService: ClientService,
                                         val scopeService: ScopeService,
                                         val tokenService: TokenService) extends TokenOrchestrator

trait TokenOrchestrator {

  protected val grantService: GrantService
  protected val tokenService: TokenService
  protected val userService: UserService
  protected val clientService: ClientService
  protected val scopeService: ScopeService

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def authorizationCodeGrant(authCode: String, app: RegisteredApplication, redirectUri: String, codeVerifier: Option[String])
                            (implicit ec: ExC): Future[TokenResponse] = {
    grantService.validateGrant(authCode, app.clientId, redirectUri, codeVerifier) flatMap {
      case Some(grant) =>
        logger.info("[authorizationCodeGrant] - Grant has been validated")
        val scopes = scopeService.getScopeDetails(grant.scope)

        userService.getUserInfo(grant.userId).flatMap {
          case Some(userData) =>
            val clientId = grant.clientId.decrypt.getOrElse(grant.clientId)
            if(app.oauth2Flows.contains("authorization_code")) {
              logger.info(s"[authorizationCodeGrant] - Issuing new Id and access token for ${grant.userId} for use by clientId ${grant.clientId}")
              val scopedData = userData.toMap.filter{ case (k, _) => scopes.exists(_.name == k)}
              val tokenSetId = tokenService.generateTokenRecordSetId
              val accessId = tokenService.generateTokenRecordSetId
              val idId = tokenService.generateTokenRecordSetId
              val refreshId = tokenService.generateTokenRecordSetId

              val accessToken = tokenService.createAccessToken(clientId, grant.userId, tokenSetId, accessId, grant.scope.mkString(","), app.accessTokenExpiry)
              val idToken = if(scopes.exists(_.name == "openid")) {
                Some(tokenService.createIdToken(clientId, grant.userId, tokenSetId, idId, scopedData, app.idTokenExpiry))
              } else {
                None
              }

              val refreshToken = tokenService.createRefreshToken(clientId, grant.userId, app.refreshTokenExpiry, tokenSetId, refreshId, grant.scope)

              tokenService.createTokenRecordSet(tokenSetId, grant.userId, app.appId, accessId, idToken.map(_ => idId), None) map {
                case MongoSuccessCreate =>
                  Issued(
                    tokenType = "Bearer",
                    scope = grant.scope.mkString(","),
                    expiresIn = app.accessTokenExpiry,
                    accessToken,
                    idToken,
                    Some(refreshToken)
                  )
                case MongoFailedCreate => TokenError
              }
            } else {
              logger.warn(s"[authorizationCodeGrant] - The app ${app.appId} is not allowed to obtain client creds")
              Future.successful(InvalidOAuthFlow)
            }
          case None =>
            logger.warn(s"[authorizationCodeGrant] - could not validate user on userId ${grant.userId}")
            Future.successful(InvalidUser)
        }
      case None =>
        logger.warn(s"[authorizationCodeGrant] - Couldn't validate grant")
        Future.successful(InvalidGrant)
    }
  }

  def clientCredentialsGrant(app: RegisteredApplication, scope: String)(implicit ec: ExC): Future[TokenResponse] = {
    if(app.oauth2Flows.contains("client_credentials")) {
      logger.info(s"[clientCredentialsGrant] - Issuing new access token for client ${app.appId} for use by client ${app.appId}")

      val tokenSetId = tokenService.generateTokenRecordSetId
      val accessId = tokenService.generateTokenRecordSetId
      val accessToken = tokenService.createClientAccessToken(app.clientId, tokenSetId, accessId, app.accessTokenExpiry)

      tokenService.createTokenRecordSet(
        tokenSetId,
        app.appId,
        app.appId,
        accessId,
        None,
        None) map {
        case MongoSuccessCreate =>
          Issued(
            tokenType = "Bearer",
            scope,
            expiresIn = tokenService.expiry,
            accessToken,
            idToken = None,
            refreshToken = None
          )
        case MongoFailedCreate => TokenError
      }
    } else {
      logger.warn(s"[clientCredentialsGrant] - The app ${app.appId} is not allowed to obtain client creds")
      Future.successful(InvalidOAuthFlow)
    }
  }

  def refreshTokenGrant(app: RegisteredApplication, token: String)(implicit ec: ExC): Future[TokenResponse] = {
    RefreshToken.dec(token).fold(
      err => {
        logger.warn(s"[refreshTokenGrant] - There was a problem decrypting the refresh token", err)
        Future.successful(TokenError)
      },
      refreshToken => {
        val scopes = scopeService.getScopeDetails(refreshToken.scope)

        userService.getUserInfo(refreshToken.sub) flatMap {
          case Some(userData) =>
            if(app.oauth2Flows.contains("refresh_token")) {
              val scopedData = userData.toMap.filter{ case (k, _) => scopes.exists(_.name == k)}
              val accessId = tokenService.generateTokenRecordSetId
              val idId = tokenService.generateTokenRecordSetId
              val accessToken = tokenService.createAccessToken(
                refreshToken.aud, refreshToken.sub, refreshToken.tsid, accessId, refreshToken.scope.mkString(","), app.accessTokenExpiry
              )
              val idToken = if(refreshToken.scope.contains("openid")) {
                Some(tokenService.createIdToken(refreshToken.aud, refreshToken.sub, refreshToken.tsid, idId, scopedData, app.idTokenExpiry))
              } else {
                None
              }

              tokenService.updateTokenRecordSet(refreshToken.tsid, accessId, idId) map {
                case MongoSuccessUpdate =>
                  Issued(
                    tokenType = "Bearer",
                    scope = refreshToken.scope.mkString(","),
                    expiresIn = tokenService.expiry,
                    accessToken,
                    idToken,
                    refreshToken = Some(token)
                  )
                case MongoFailedUpdate => TokenError
              }
            } else {
              logger.warn(s"[refreshTokenGrant] - The app ${refreshToken.aud} is not allowed to get access tokens via refresh tokens")
              Future.successful(InvalidOAuthFlow)
            }
          case None =>
            logger.warn(s"[refreshTokenGrant] - could not validate user on userId ${refreshToken.sub}")
            Future.successful(InvalidUser)
        }
      }
    )
  }

  def revokeTokens(tokenRecordSet: String, userId: String, appId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    tokenService.revokeTokens(tokenRecordSet, appId, userId)
  }

  def revokeToken(token: String, tokenType: Option[String])(implicit ec: ExC): Future[Either[MongoUpdatedResponse, MongoDeleteResponse]] = {
    val (setId, tokenId) = tokenService.extractTokenIds(token)
    tokenService.revokeSpecificToken(tokenType, setId, tokenId)
  }
}
