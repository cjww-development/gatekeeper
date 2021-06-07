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

package global

import akka.util.ByteString
import controllers.ui.routes
import dev.cjww.http.request.RequestErrorHandler
import play.api.http.{HttpErrorHandler, Status, Writeable}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, RequestHeader, Result}
import play.twirl.api.Html
import views.html.misc.{INS, NotFound}

import javax.inject.Inject
import scala.concurrent.Future

class ErrorHandler @Inject()() extends HttpErrorHandler {
  val frontendErrorHandler: RequestErrorHandler[Html] = new RequestErrorHandler[Html] {
    override implicit val writer: Writeable[Html] = Writeable(html => ByteString(html.body), contentType = Some("text/html"))
    override def standardError(status: Int)(implicit rh: RequestHeader): Html = INS()
    override def notFoundError(implicit rh: RequestHeader): Html = NotFound()
    override def serverError(implicit rh: RequestHeader): Html = INS()
    override def forbiddenError(implicit rh: RequestHeader): Either[Html, Call] = Right(Call("GET", routes.LoginController.logout().url))
  }

  val apiErrorHandler: RequestErrorHandler[JsValue] = new RequestErrorHandler[JsValue] {
    val jsonResponse: (Int, String) => JsValue = (status, msg) => Json.obj(
      "status" -> status,
      "msg" -> msg
    )

    override implicit val writer: Writeable[JsValue] = Writeable.writeableOf_JsValue
    override def standardError(status: Int)(implicit rh: RequestHeader): JsValue = jsonResponse(status, "Something went wrong")
    override def notFoundError(implicit rh: RequestHeader): JsValue = jsonResponse(Status.NOT_FOUND, "Resource not found")
    override def serverError(implicit rh: RequestHeader): JsValue = jsonResponse(Status.INTERNAL_SERVER_ERROR, "Something went wrong")
    override def forbiddenError(implicit rh: RequestHeader): Either[JsValue, Call] = Left(jsonResponse(Status.FORBIDDEN, "You are not authorised to access this resource"))
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if(request.path.contains("/gatekeeper/api")) {
      apiErrorHandler.onClientError(request, statusCode, message)
    } else {
      frontendErrorHandler.onClientError(request, statusCode, message)
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    if(request.path.contains("/gatekeeper/api")) {
      apiErrorHandler.onServerError(request, exception)
    } else {
      frontendErrorHandler.onServerError(request, exception)
    }
  }
}
