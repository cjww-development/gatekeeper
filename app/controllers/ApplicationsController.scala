/*
 * Copyright 2019 CJWW Development
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

package controllers

import com.cjwwdev.config.ConfigurationLoader
import com.cjwwdev.featuremanagement.services.FeatureService
import database.responses.{MySQLFailedCreate, MySQLFailedDelete, MySQLSuccessCreate, MySQLSuccessDelete}
import global.Features
import javax.inject.Inject
import models.RegisteredApplication
import models.RegisteredApplicationCompanion._
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ApplicationService

import scala.concurrent.ExecutionContext

class DefaultApplicationsController @Inject()(val controllerComponents: ControllerComponents,
                                              val featureService: FeatureService,
                                              val applicationService: ApplicationService,
                                              val config: ConfigurationLoader) extends ApplicationsController {
  override val adminId: String = config.getServiceId("admin-frontend")
  override val appId: String   = config.getServiceId("gatekeeper")
  override implicit val ec: ExecutionContext = controllerComponents.executionContext
}

trait ApplicationsController extends BackendController {

  val applicationService: ApplicationService

  def registerApplication(): Action[String] = Action.async(parse.text) { implicit req =>
    apiFeatureGuard(Features.registration) {
      applicationVerification {
        withJsonBody[RegisteredApplication] { app =>
          applicationService.registerNewApplication(app) map { res =>
            val (status, body) = res match {
              case MySQLSuccessCreate => (CREATED, JsString(s"Registered new application ${app.name}"))
              case MySQLFailedCreate  => (BAD_REQUEST, JsString(s"There was a problem creating the application ${app.name}"))
            }

            withJsonResponseBody(status, body) {
              json => Status(status)(json)
            }
          }
        }
      }
    }
  }

  def getAllApplications(): Action[AnyContent] = Action.async { implicit req =>
    applicationVerification {
      applicationService.getAllApplications map { res =>
        val (status, body) = res.fold(
          _   => (NO_CONTENT, JsString("")),
          seq => (OK, Json.toJson(seq))
        )

        withJsonResponseBody(status, body) {
          json => Status(status)(json)
        }
      }
    }
  }

  def removeApplication(name: String): Action[AnyContent] = Action.async { implicit req =>
    apiFeatureGuard(Features.registration) {
      applicationVerification {
        applicationService.removeRegisteredApplication(name) map { res =>
          val (status, body) = res match {
            case MySQLSuccessDelete => (NO_CONTENT, s"Application $name has been deleted")
            case MySQLFailedDelete  => (BAD_REQUEST, s"There was a problem removing the application $name")
          }

          withJsonResponseBody(status, body) {
            json => Status(status)(json)
          }
        }
      }
    }
  }
}
