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

package services.comms.email

import database.VerificationStore
import models.{EmailResponse, Verification}
import dev.cjww.security.Implicits._
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.http.MimeTypes
import play.api.libs.ws.{WSAuthScheme, WSClient}
import play.api.mvc.Request
import play.mvc.Http.HeaderNames
import views.html.email.VerificationEmail

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultMailgunService @Inject()(val config: Configuration,
                                      val wsClient: WSClient,
                                      val verificationStore: VerificationStore) extends MailgunService {
  override val emailSenderAddress: String = config.get[String]("email-service.message-settings.from")
  override val verificationSubjectLine: String = config.get[String]("email-service.message-settings.verification-subject")
  override val apiKey: String = config.get[String]("email-service.mail-gun.api-key")
  override val mailgunUrl: String = config.get[String]("email-service.mail-gun.url")
}

trait MailgunService extends EmailService {

  val apiKey: String
  val mailgunUrl: String

  val wsClient: WSClient

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def sendEmailVerificationMessage(to: String, record: Verification)(implicit req: Request[_], ec: ExC): Future[EmailResponse] = {
    val queryParam = record.encrypt
    val formData = Map(
      "from" -> Seq(emailSenderAddress),
      "to" -> Seq(to),
      "subject" -> Seq(verificationSubjectLine),
      "html" -> Seq(VerificationEmail(queryParam).body)
    )

    wsClient
      .url(mailgunUrl)
      .withAuth("api", apiKey, WSAuthScheme.BASIC)
      .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.FORM)
      .post(formData)
      .map { resp =>
        val msgId = resp.json.\("id").as[String]
        logger.info(s"[sendEmailVerificationMessage] - Welcome email sent with messageId $msgId to userId ${record.userId} via AWS SES")
        EmailResponse("mailgun", record.userId, msgId)
      } recover {
        case e =>
          logger.error("[sendEmailVerificationMessage] - There was a problem sending the welcome email via Mailgun", e)
          EmailResponse("ses", record.userId, "DID NOT SEND")
      }
  }
}
