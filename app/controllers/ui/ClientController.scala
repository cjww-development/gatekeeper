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

import controllers.actions.AuthenticatedFilter
import forms.AppRegistrationForm.{form => appRegForm}
import forms.RegistrationForm.{form => regForm, _}
import javax.inject.Inject
import orchestrators._
import org.slf4j.LoggerFactory
import play.api.i18n.{I18NSupportLowPriorityImplicits, I18nSupport, Lang}
import play.api.mvc._
import views.html.registration.{AppRegistration, UserRegistration}
import views.html.client.{ClientView, ClientsView}
import views.html.misc.{NotFound => NotFoundView}

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultClientController @Inject()(val controllerComponents: ControllerComponents,
                                        val userOrchestrator: UserOrchestrator,
                                        val clientOrchestrator: ClientOrchestrator,
                                        val registrationOrchestrator: RegistrationOrchestrator) extends ClientController {
  override implicit val ec: ExC = controllerComponents.executionContext
}

trait ClientController extends BaseController with I18NSupportLowPriorityImplicits with I18nSupport with AuthenticatedFilter {

  val registrationOrchestrator: RegistrationOrchestrator
  val clientOrchestrator: ClientOrchestrator

  implicit val ec: ExC

  implicit def langs(implicit rh: RequestHeader): Lang = messagesApi.preferred(rh).lang

  private val logger = LoggerFactory.getLogger(this.getClass)

  def showAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => userId =>
    Future.successful(Ok(AppRegistration(appRegForm(userId))))
  }

  def submitAppReg(): Action[AnyContent] = authenticatedOrgUser { implicit req => userId =>
    appRegForm(userId).bindFromRequest.fold(
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
}
