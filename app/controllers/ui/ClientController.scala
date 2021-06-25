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

package controllers.ui

import controllers.actions.AuthenticatedAction
import dev.cjww.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import forms.AppRegistrationForm.{form => appRegForm}
import models.TokenExpiry
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import services.oauth2.ScopeService
import views.html.client._
import views.html.misc.{INS, NotFound => NotFoundView}
import views.html.registration.AppRegistration

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.util.Try

class DefaultClientController @Inject()(val controllerComponents: ControllerComponents,
                                        val scopeService: ScopeService,
                                        val userOrchestrator: UserOrchestrator,
                                        val clientOrchestrator: ClientOrchestrator,
                                        val tokenOrchestrator: TokenOrchestrator,
                                        val registrationOrchestrator: RegistrationOrchestrator) extends ClientController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait ClientController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedAction {

  val registrationOrchestrator: RegistrationOrchestrator
  val clientOrchestrator: ClientOrchestrator
  val scopeService: ScopeService
  val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  def showAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => userId =>
    Future.successful(Ok(AppRegistration(appRegForm(userId))))
  }

  def submitAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => userId =>
    appRegForm(userId).bindFromRequest().fold(
      errs => Future.successful(BadRequest(errs.toString)),
      app  => registrationOrchestrator.registerApplication(app) map {
        case AppRegistered        => Redirect(routes.ClientController.getClientDetails(app.appId))
        case AppRegistrationError => InternalServerError
      }
    )
  }

  def getClientDetails(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    for {
      app <- clientOrchestrator.getRegisteredApp(orgUserId, appId)
      expiry <- clientOrchestrator.getTokenExpiry(appId, orgUserId)
    } yield if(app.isDefined && expiry.isDefined) {
      Ok(ClientView(app.get, expiry.get, scopeService.getValidScopes))
    } else {
      NotFound(NotFoundView())
    }
  }

  def getAllClients(groupedBy: Int): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    clientOrchestrator.getRegisteredApps(orgUserId, groupedBy) map { apps =>
      Ok(ClientsView(apps))
    }
  }

  def regenerateIdAndSecretShow(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    clientOrchestrator.getRegisteredApp(orgUserId, appId) map {
      case Some(app) => Ok(RegenerateClientIdView(app))
      case None      => NotFound(NotFoundView())
    }
  }

  def regenerateIdAndSecretSubmit(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    clientOrchestrator.regenerateClientIdAndSecret(orgUserId, appId) map {
      case SecretsUpdated => Redirect(routes.ClientController.getClientDetails(appId))
      case NoAppFound => NotFound(NotFoundView())
      case _ => InternalServerError(INS())
    }
  }

  def deleteClientShow(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    clientOrchestrator.getRegisteredApp(orgUserId, appId) map {
      case Some(app) => Ok(DeleteClientView(app))
      case None      => NotFound(NotFoundView())
    }
  }

  def deleteClientSubmit(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    clientOrchestrator.deleteClient(orgUserId, appId) map {
      case MongoSuccessDelete => Redirect(routes.ClientController.getAllClients())
      case MongoFailedDelete  => NotFound(NotFoundView())
    }
  }

  def getAuthorisedAppsForUser(): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    clientOrchestrator.getAuthorisedApps(userId) map {
      apps => Ok(AuthorisedClientsView(apps))
    }
  }

  def getAuthorisedApp(appId: String): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    clientOrchestrator.getAuthorisedApp(userId, appId) map {
      case Some((app, client, sessions)) => Ok(AuthorisedClientView(app, client, scopeService.getValidScopes, sessions))
      case None      => NotFound(NotFoundView())
    }
  }

  def revokeAppAccess(appId: String): Action[AnyContent] = authenticatedUser { _ => userId =>
    clientOrchestrator.unlinkAppFromUser(appId, userId) map {
      _ => Redirect(routes.ClientController.getAuthorisedAppsForUser())
    }
  }

  def revokeSession(tokenSetId: String, appId: String): Action[AnyContent] = authenticatedUser { _ => userId =>
    tokenOrchestrator.revokeTokens(tokenSetId, userId, appId) map {
      _ => Redirect(routes.ClientController.getAuthorisedApp(appId))
    }
  }

  def updateOAuthFlows(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    val reqBody = req.body.asFormUrlEncoded.getOrElse(Map())
    val oauthFlows = reqBody.getOrElse("auth-code-check", Seq()) ++ reqBody.getOrElse("client-cred-check", Seq()) ++ reqBody.getOrElse("refresh-check", Seq())

    clientOrchestrator.updateAppOAuthFlows(oauthFlows, appId, orgUserId) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateOAuthScopes(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    val reqBody = req.body.asFormUrlEncoded.getOrElse(Map())
    val oauthScopes = scopeService.getValidScopes.flatMap { scope =>
      reqBody.getOrElse(s"${scope.name}-check", Seq())
    }

    clientOrchestrator.updateAppOAuthScopes(oauthScopes, appId, orgUserId) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateTokenExpiry(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val expiry = TokenExpiry(
      idTokenMins = Try(body.getOrElse("id-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0),
      idTokenDays = Try(body.getOrElse("id-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      accessTokenDays = Try(body.getOrElse("access-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      accessTokenMins = Try(body.getOrElse("access-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0),
      refreshTokenDays = Try(body.getOrElse("refresh-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      refreshTokenMins = Try(body.getOrElse("refresh-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0)
    )

    clientOrchestrator.updateTokenExpiry(appId, orgUserId, expiry) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateHomeAndRedirect(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => orgUserId =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val homeUrl = body.getOrElse("home-url", Seq("")).head
    val redirectUrl = body.getOrElse("redirect-url", Seq("")).head

    clientOrchestrator.updateRedirects(appId, orgUserId, homeUrl, redirectUrl) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }
}
