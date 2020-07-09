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
import com.cjwwdev.http.responses.ApiResponse
import global.Logging
import play.api.libs.json.{JsString, JsValue, Reads}
import play.api.mvc.{BaseController, Request, Result}

import scala.concurrent.{Future, ExecutionContext => ExC}

trait BackendController
  extends BaseController
    with ApiResponse
    with Logging {

  val featureService: FeatureService

  implicit val ec: ExC

  protected def withJsonBody[T](f: T => Future[Result])(implicit req: Request[JsValue], reads: Reads[T]): Future[Result] = {
    f(req.body.as[T])
  }

  protected def apiFeatureGuard(feature: String)(f: => Future[Result])(implicit req: Request[_]): Future[Result] = {
    featureService.getState(feature) match {
      case Some(_) => f
      case None =>
        logger.warn(s"[apiFeatureGuard] - API on path ${req.path} is currently disabled; returning Service Unavailable")
        withJsonResponseBody(SERVICE_UNAVAILABLE, JsString("This service is unavailable, please try again later")) {
          json => Future.successful(ServiceUnavailable(json))
        }
    }
  }
}
