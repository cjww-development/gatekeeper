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

package controllers.ui

import com.cjwwdev.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import controllers.actions.AuthenticatedFilter
import forms.AppRegistrationForm.{form => appRegForm}
import javax.inject.Inject
import orchestrators._
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import services.ScopeService
import views.html.registration.AppRegistration
import views.html.client.{AuthorisedClientView, AuthorisedClientsView, ClientView, ClientsView, DeleteClientView, RegenerateClientIdView}
import views.html.misc.{NotFound => NotFoundView}

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultClientController @Inject()(val controllerComponents: ControllerComponents,
                                        val scopeService: ScopeService,
                                        val userOrchestrator: UserOrchestrator,
                                        val clientOrchestrator: ClientOrchestrator,
                                        val tokenOrchestrator: TokenOrchestrator,
                                        val registrationOrchestrator: RegistrationOrchestrator) extends ClientController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait ClientController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedFilter {

  val registrationOrchestrator: RegistrationOrchestrator
  val clientOrchestrator: ClientOrchestrator
  val scopeService: ScopeService
  val tokenOrchestrator: TokenOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

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
    clientOrchestrator.getRegisteredApp(orgUserId, appId) map {
      case Some(app) => Ok(ClientView(app))
      case None      => NotFound(NotFoundView())
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

  def revokeAppAccess(appId: String): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    clientOrchestrator.unlinkAppFromUser(appId, userId) map {
      _ => Redirect(routes.ClientController.getAuthorisedAppsForUser())
    }
  }

  def revokeSession(tokenSetId: String, appId: String): Action[AnyContent] = authenticatedUser { implicit req => userId =>
    tokenOrchestrator.revokeTokens(tokenSetId, userId, appId) map {
      _ => Redirect(routes.ClientController.getAuthorisedApp(appId))
    }
  }
}
