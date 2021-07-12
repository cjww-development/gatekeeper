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

package services.comms.email

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}
import database.VerificationStore
import dev.cjww.security.Implicits._
import models.{EmailResponse, Verification}
import play.api.Configuration
import play.api.mvc.Request
import views.html.email.VerificationEmail

import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultSesService @Inject()(val config: Configuration,
                                  val verificationStore: VerificationStore) extends SesService {
  override val awsRegion: String = config.get[String]("email-service.ses.region")
  override val emailSenderAddress: String = config.get[String]("email-service.message-settings.from")
  override val verificationSubjectLine: String = config.get[String]("email-service.message-settings.verification-subject")

  override val emailClient: AmazonSimpleEmailService = AmazonSimpleEmailServiceClientBuilder
    .standard()
    .withRegion(Regions.fromName(awsRegion))
    .build()
}

trait SesService extends EmailService {
  val awsRegion: String
  val emailClient: AmazonSimpleEmailService

  override def sendEmailVerificationMessage(to: String, record: Verification)(implicit req: Request[_], ec: ExC): Future[EmailResponse] = {
    val queryParam = record.encrypt

    val destination: Destination = new Destination()
      .withToAddresses(to)

    val subjectContent: Content = new Content()
      .withCharset(StandardCharsets.UTF_8.name())
      .withData(verificationSubjectLine)

    val bodyContent: Content = new Content()
      .withCharset(StandardCharsets.UTF_8.name())
      .withData(VerificationEmail(queryParam).body)

    val body: Body = new Body()
      .withHtml(bodyContent)

    val message: Message = new Message()
      .withBody(body)
      .withSubject(subjectContent)

    val request: SendEmailRequest = new SendEmailRequest()
      .withDestination(destination)
      .withMessage(message)
      .withSource(emailSenderAddress)

    val resp = emailClient.sendEmail(request)
    Future.successful(EmailResponse("ses", record.userId, resp.getMessageId))
  }
}
