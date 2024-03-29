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

package controllers.ui

import controllers.actions.AuthenticatedAction
import dev.cjww.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import forms.AppRegistrationForm.{form => appRegForm}
import models.TokenExpiry
import orchestrators._
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import services.oauth2.{ClientService, ScopeService}
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
                                        val clientService: ClientService,
                                        val tokenOrchestrator: TokenOrchestrator,
                                        val registrationOrchestrator: RegistrationOrchestrator) extends ClientController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait ClientController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedAction {

  val registrationOrchestrator: RegistrationOrchestrator
  val clientOrchestrator: ClientOrchestrator
  val scopeService: ScopeService
  val clientService: ClientService
  val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  def showAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val presetService = clientService.getPresetServices.map(_.name)
    Future.successful(Ok(AppRegistration(appRegForm(user.id), presetService)))
  }

  def submitPreset(): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val reqBody = req.body.asFormUrlEncoded.getOrElse(Map())
    val presetChoice = reqBody.get("preset-choice").map(_.head)
    presetChoice match {
      case Some(preset) => registrationOrchestrator.registerPresetApplication(user.id, preset) map {
        case AppRegistered(id) => Redirect(routes.ClientController.getClientDetails(id))
        case AppRegistrationError => InternalServerError(INS())
      }
      case None => Future.successful(InternalServerError(INS()))
    }
  }

  def submitAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    appRegForm(user.id).bindFromRequest().fold(
      errs => Future.successful(BadRequest(errs.toString)),
      app  => registrationOrchestrator.registerApplication(app) map {
        case AppRegistered        => Redirect(routes.ClientController.getClientDetails(app.appId))
        case AppRegistrationError => InternalServerError
      }
    )
  }

  def getClientDetails(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    for {
      app <- clientOrchestrator.getRegisteredApp(user.id, appId)
      expiry <- clientOrchestrator.getTokenExpiry(appId, user.id)
    } yield if(app.isDefined && expiry.isDefined) {
      Ok(ClientView(app.get, expiry.get, scopeService.getValidScopes))
    } else {
      NotFound(NotFoundView())
    }
  }

  def getAllClients(groupedBy: Int): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    clientOrchestrator.getRegisteredApps(user.id, groupedBy) map { apps =>
      Ok(ClientsView(apps))
    }
  }

  def regenerateIdAndSecretShow(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    clientOrchestrator.getRegisteredApp(user.id, appId) map {
      case Some(app) => Ok(RegenerateClientIdView(app))
      case None      => NotFound(NotFoundView())
    }
  }

  def regenerateIdAndSecretSubmit(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    clientOrchestrator.regenerateClientIdAndSecret(user.id, appId) map {
      case SecretsUpdated => Redirect(routes.ClientController.getClientDetails(appId))
      case NoAppFound => NotFound(NotFoundView())
      case _ => InternalServerError(INS())
    }
  }

  def deleteClientShow(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    clientOrchestrator.getRegisteredApp(user.id, appId) map {
      case Some(app) => Ok(DeleteClientView(app))
      case None      => NotFound(NotFoundView())
    }
  }

  def deleteClientSubmit(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    clientOrchestrator.deleteClient(user.id, appId) map {
      case MongoSuccessDelete => Redirect(routes.ClientController.getAllClients())
      case MongoFailedDelete  => NotFound(NotFoundView())
    }
  }

  def getAuthorisedAppsForUser(): Action[AnyContent] = authenticatedUser { implicit req => user =>
    clientOrchestrator.getAuthorisedApps(user.id) map {
      apps => Ok(AuthorisedClientsView(apps))
    }
  }

  def getAuthorisedApp(appId: String): Action[AnyContent] = authenticatedUser { implicit req => user =>
    clientOrchestrator.getAuthorisedApp(user.id, appId) map {
      case Some((app, client, sessions)) => Ok(AuthorisedClientView(app, client, scopeService.getValidScopes, sessions))
      case None      => NotFound(NotFoundView())
    }
  }

  def revokeAppAccess(appId: String): Action[AnyContent] = authenticatedUser { _ => user =>
    clientOrchestrator.unlinkAppFromUser(appId, user.id) map {
      _ => Redirect(routes.ClientController.getAuthorisedAppsForUser())
    }
  }

  def revokeSession(tokenSetId: String, appId: String): Action[AnyContent] = authenticatedUser { _ => user =>
    tokenOrchestrator.revokeTokens(tokenSetId, user.id, appId) map {
      _ => Redirect(routes.ClientController.getAuthorisedApp(appId))
    }
  }

  def updateOAuthFlows(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val reqBody = req.body.asFormUrlEncoded.getOrElse(Map())
    val oauthFlows = reqBody.getOrElse("auth-code-check", Seq()) ++ reqBody.getOrElse("client-cred-check", Seq()) ++ reqBody.getOrElse("refresh-check", Seq())

    clientOrchestrator.updateAppOAuthFlows(oauthFlows, appId, user.id) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateOAuthScopes(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val reqBody = req.body.asFormUrlEncoded.getOrElse(Map())
    val oauthScopes = scopeService.getValidScopes.flatMap { scope =>
      reqBody.getOrElse(s"${scope.name}-check", Seq())
    }

    clientOrchestrator.updateAppOAuthScopes(oauthScopes, appId, user.id) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateTokenExpiry(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val expiry = TokenExpiry(
      idTokenMins = Try(body.getOrElse("id-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0),
      idTokenDays = Try(body.getOrElse("id-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      accessTokenDays = Try(body.getOrElse("access-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      accessTokenMins = Try(body.getOrElse("access-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0),
      refreshTokenDays = Try(body.getOrElse("refresh-token-days", Seq("0")).map(_.toLong).head).getOrElse(0),
      refreshTokenMins = Try(body.getOrElse("refresh-token-mins", Seq("0")).map(_.toLong).head).getOrElse(0)
    )

    clientOrchestrator.updateTokenExpiry(appId, user.id, expiry) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateHomeAndRedirect(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val homeUrl = body.getOrElse("home-url", Seq("")).head
    val redirectUrl = body.getOrElse("redirect-url", Seq("")).head

    clientOrchestrator.updateRedirects(appId, user.id, homeUrl, redirectUrl) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }

  def updateBasicDetails(appId: String): Action[AnyContent] = authenticatedOrgUser { implicit req => user =>
    val body = req.body.asFormUrlEncoded.getOrElse(Map())
    val name = body.getOrElse("name", Seq("")).head
    val desc = body.getOrElse("desc", Seq("")).head
    val iconUrl = body.get("icon-url").filter(_.head != "").map(_.head)

    clientOrchestrator.updateBasicDetails(appId, user.id, name, desc, iconUrl) map {
      _ => Redirect(routes.ClientController.getClientDetails(appId))
    }
  }
}
