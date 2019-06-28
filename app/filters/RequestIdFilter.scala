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

package filters

import java.util.UUID

import akka.stream.Materializer
import javax.inject.Inject
import play.api.mvc.{Filter, Headers, RequestHeader, Result}

import scala.concurrent.Future

class RequestIdFilter @Inject()(implicit val mat: Materializer) extends Filter {

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    rh.headers.get("requestId") match {
      case Some(_) => f(rh)
      case None    => f(rh.withHeaders(Headers(
        rh.headers.headers ++ Seq("requestId" -> s"requestId-${UUID.randomUUID()}"):_*
      )))
    }
  }
}
