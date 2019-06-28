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

import com.cjwwdev.auth.backend.{Authenticated, AuthorisationResult, NotAuthorised}
import com.cjwwdev.featuremanagement.services.FeatureService
import com.cjwwdev.http.headers.HttpHeaders
import com.cjwwdev.logging.output.Logger
import com.cjwwdev.request.RequestParsers
import com.cjwwdev.responses.ApiResponse
import play.api.libs.json.JsString
import play.api.mvc.{BaseController, Request, Result}

import scala.concurrent.{ExecutionContext => ExC, Future}

trait BackendController
  extends BaseController
    with RequestParsers
    with ApiResponse
    with Logger
    with HttpHeaders{

  val adminId: String

  val featureService: FeatureService

  implicit val ec: ExC

  protected def apiFeatureGuard(feature: String)(f: => Future[Result])(implicit req: Request[_]): Future[Result] = {
    if(featureService.getState(feature).state) {
      f
    } else {
      LogAt.warn(s"[apiFeatureGuard] - API on path ${req.path} is currently disabled; returning Service Unavailable")
      withFutureJsonResponseBody(SERVICE_UNAVAILABLE, JsString("This service is unavailable, please try again later")) {
        json => Future.successful(ServiceUnavailable(json))
      }
    }
  }

  protected def applicationVerification(f: => Future[Result])(implicit req: Request[_]): Future[Result] = {
    validateAppId match {
      case Authenticated  => f
      case _ => withFutureJsonResponseBody(FORBIDDEN, "The calling application could not be verified") { json =>
        Future.successful(Forbidden(json))
      }
    }
  }

  protected def validateAppId(implicit req: Request[_]): AuthorisationResult = {
    constructHeaderPackageFromRequestHeaders.fold(notAuthorised("AppID not found in the header package"))( headerPackage =>
      if(adminId == headerPackage.appId) Authenticated else notAuthorised("API CALL FROM UNKNOWN SOURCE - ACTION DENIED")
    )
  }

  private def notAuthorised(msg: String)(implicit req: Request[_]): AuthorisationResult = {
    LogAt.error(s"[applicationVerification] - $msg")
    NotAuthorised
  }
}
