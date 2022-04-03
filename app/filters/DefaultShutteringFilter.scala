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

package filters

import akka.stream.Materializer
import akka.util.ByteString
import dev.cjww.shuttering.filters.ShutteringFilter
import play.api.Configuration
import play.api.http.Writeable
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Filter, RequestHeader, Result}
import play.twirl.api.Html
import views.html.misc.Shutter

import javax.inject.Inject
import scala.concurrent.Future

class DefaultShutteringFilter @Inject()(val config: Configuration,
                                        val materializer: Materializer) extends Filter {

  override implicit def mat: Materializer = materializer

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val uiShutter = new ShutteringFilter[Html] {
      override def shutterResponse(statusCode: Int)(implicit rh: RequestHeader): Html = Shutter()
      override implicit val writer: Writeable[Html] = Writeable(html => ByteString(html.body), contentType = Some("text/html"))
      override val appName: String = config.get[String]("appName")
      override implicit def mat: Materializer = materializer
    }

    val apiShutter = new ShutteringFilter[JsValue] {
      override def shutterResponse(statusCode: Int)(implicit rh: RequestHeader): JsValue = Json.obj(
        "status" -> statusCode,
        "msg" -> "This service is currently shuttered. Come back later"
      )
      override implicit val writer: Writeable[JsValue] = Writeable.writeableOf_JsValue
      override val appName: String = config.get[String]("appName")
      override implicit def mat: Materializer = materializer
    }

    if(rh.uri.contains("/gatekeeper/api")) apiShutter(f)(rh) else uiShutter(f)(rh)
  }
}
