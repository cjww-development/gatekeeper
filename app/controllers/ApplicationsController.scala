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

import com.cjwwdev.featuremanagement.services.FeatureService
import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoFailedDelete, MongoSuccessCreate, MongoSuccessDelete}
import javax.inject.Inject
import models.RegisteredApplication
import models.RegisteredApplication._
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ApplicationService

import scala.concurrent.ExecutionContext

class DefaultApplicationsController @Inject()(val controllerComponents: ControllerComponents,
                                              val featureService: FeatureService,
                                              val applicationService: ApplicationService) extends ApplicationsController {
  override implicit val ec: ExecutionContext = controllerComponents.executionContext
}

trait ApplicationsController extends BackendController {

  val applicationService: ApplicationService

  def registerApplication(): Action[JsValue] = Action.async(parse.json) { implicit req =>
    withJsonBody[RegisteredApplication] { app =>
      applicationService.registerNewApplication(app) map { res =>
        val (status, body) = res match {
          case MongoSuccessCreate => (CREATED, JsString(s"Registered new application ${app.name}"))
          case MongoFailedCreate  => (BAD_REQUEST, JsString(s"There was a problem creating the application ${app.name}"))
        }

        withJsonResponseBody(status, body) {
          json => Status(status)(json)
        }
      }
    }(req, inboundReads)
  }

  def getAllApplications(): Action[AnyContent] = Action.async { implicit req =>
    applicationService.getAllApplications map { res =>
      val (status, body) = if(res.nonEmpty) {
        (OK, Json.toJson(res))
      } else {
        (NO_CONTENT, JsString(""))
      }

      withJsonResponseBody(status, body) {
        json => Status(status)(json)
      }
    }
  }

  def getOneServiceByName(name: String): Action[AnyContent] = Action.async { implicit req =>
    applicationService.getServiceByName("name", name) map { app =>
      val (status, body) = app match {
        case Some(srv) => (OK, Json.toJson(srv))
        case None      => (NOT_FOUND, JsString(s"Service $name could not be found"))
      }

      withJsonResponseBody(status, body) {
        json => Status(status)(json)
      }
    }
  }

  def removeApplication(name: String): Action[AnyContent] = Action.async { implicit req =>
    applicationService.removeRegisteredApplication(name) map { res =>
      val (status, body) = res match {
        case MongoSuccessDelete => (NO_CONTENT, JsString(s"Application $name has been deleted"))
        case MongoFailedDelete  => (BAD_REQUEST, JsString(s"There was a problem removing the application $name"))
      }

      withJsonResponseBody(status, body) {
        json => Status(status)(json)
      }
    }
  }
}
