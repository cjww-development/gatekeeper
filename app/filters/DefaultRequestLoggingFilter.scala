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
import org.joda.time.DateTimeUtils
import org.slf4j.{Logger, LoggerFactory, MDC}
import play.api.Configuration
import play.api.mvc._
import play.utils.Colors

import scala.concurrent.{ExecutionContext, Future}

class DefaultRequestLoggingFilter @Inject()(val config: Configuration,
                                            implicit val ec: ExecutionContext,
                                            implicit val mat: Materializer) extends RequestLoggingFilter {
  override protected val colouredOutput: Boolean = config.getOptional[Boolean]("logging.coloured").getOrElse(false)
}

trait RequestLoggingFilter extends Filter {

  protected val colouredOutput: Boolean

  implicit val ec: ExecutionContext

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def apply(f: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
    val startTime = DateTimeUtils.currentTimeMillis()
    MDC.put("requestId", s"requestId-${UUID.randomUUID().toString}")
    val result = f(rh)
    result map { res =>
      logRequest(res.header.status, startTime, rh).foreach(msg => logger.info(msg))
      res
    }
  }

  def logRequest(status: Int, startTime: Long, rh: RequestHeader): Option[String] = {
    if(!rh.path.contains("/assets/")) {
      Some(
        s"${colouredMethod(rh.method.toUpperCase)} request to ${colouredPath(rh.path)} " +
          s"returned a ${colouredStatus(status)} " +
          s"and took ${colouredResponseTime(DateTimeUtils.currentTimeMillis - startTime)}ms"
      )
    } else {
      None
    }
  }

  private val colouredMethod: String => String = method => if(colouredOutput) Colors.yellow(method) else method
  private val colouredPath: String => String = path => if(colouredOutput) Colors.green(path) else path
  private val colouredStatus: Int => String = status => if(colouredOutput) Colors.cyan(s"$status") else s"$status"
  private val colouredResponseTime: Long => String = time => if(colouredOutput) Colors.magenta(s"$time") else s"$time"
}
