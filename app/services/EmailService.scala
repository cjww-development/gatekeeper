/*
 * Copyright 2020 CJWW Development
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

package services

import java.nio.charset.StandardCharsets

import javax.inject.Inject
import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.{Body, Content, Destination, Message, SendEmailRequest, SendEmailResult}
import play.api.Configuration
import views.html.email.VerificationEmail

class DefaultEmailService @Inject()(val config: Configuration) extends EmailService {
  override val emailSenderAddress: String = config.get[String]("email.from")
  override val verificationSubjectLine: String = config.get[String]("email.verification-subject")
  override val emailClient: AmazonSimpleEmailService = AmazonSimpleEmailServiceClientBuilder
    .standard()
    .withRegion(Regions.EU_WEST_1)
    .build()
}

trait EmailService {

  val emailSenderAddress: String
  val verificationSubjectLine: String

  val emailClient: AmazonSimpleEmailService

  def sendEmailVerificationMessage(to: String): SendEmailResult = {
    val destination: Destination = new Destination()
      .withToAddresses(to)

    val subjectContent: Content = new Content()
      .withCharset(StandardCharsets.UTF_8.name())
      .withData(verificationSubjectLine)

    val bodyContent: Content = new Content()
      .withCharset(StandardCharsets.UTF_8.name())
      .withData(VerificationEmail().body)

    val body: Body = new Body()
      .withHtml(bodyContent)

    val message: Message = new Message()
      .withBody(body)
      .withSubject(subjectContent)

    val request: SendEmailRequest = new SendEmailRequest()
      .withDestination(destination)
      .withMessage(message)
      .withSource(emailSenderAddress)

    emailClient.sendEmail(request)
  }
}
