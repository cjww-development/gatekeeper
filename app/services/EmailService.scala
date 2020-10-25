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
import java.util.UUID

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}
import com.cjwwdev.mongo.responses.MongoDeleteResponse
import com.cjwwdev.security.Implicits._
import database.EmailVerificationStore
import javax.inject.Inject
import models.EmailVerification
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.Configuration
import play.api.mvc.Request
import views.html.email.VerificationEmail

import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultEmailService @Inject()(val config: Configuration,
                                    val emailVerificationStore: EmailVerificationStore) extends EmailService {
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

  val emailVerificationStore: EmailVerificationStore

  def sendEmailVerificationMessage(to: String, record: EmailVerification)(implicit req: Request[_]): SendEmailResult = {
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

    emailClient.sendEmail(request)
  }

  def saveVerificationRecord(userId: String, email: String, accType: String)(implicit ec: ExC): Future[EmailVerification] = {
    val record = EmailVerification(
      verificationId = s"verify-${UUID.randomUUID().toString}",
      userId,
      email,
      accType,
      createdAt = new DateTime()
    )
    emailVerificationStore.createEmailVerificationRecord(record) map(_ => record)
  }

  def validateVerificationRecord(record: EmailVerification)(implicit ec: ExC): Future[Option[EmailVerification]] = {
    val query = and(
      equal("verificationId", record.verificationId),
      equal("userId", record.userId),
      equal("email", record.email),
    )
    emailVerificationStore.validateEmailVerificationRecord(query)
  }

  def removeVerificationRecord(verificationId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = equal("verificationId", verificationId)
    emailVerificationStore.deleteEmailVerificationRecord(query)
  }
}
