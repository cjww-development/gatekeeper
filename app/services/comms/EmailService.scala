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

package services.comms

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.model._
import com.amazonaws.services.simpleemail.{AmazonSimpleEmailService, AmazonSimpleEmailServiceClientBuilder}
import database.VerificationStore
import dev.cjww.mongo.responses.{MongoDeleteResponse, MongoFailedCreate, MongoSuccessCreate}
import dev.cjww.security.Implicits._
import models.Verification
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.Configuration
import play.api.mvc.Request
import views.html.email.VerificationEmail

import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultEmailService @Inject()(val config: Configuration,
                                    val verificationStore: VerificationStore) extends EmailService {
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

  val verificationStore: VerificationStore

  def sendEmailVerificationMessage(to: String, record: Verification)(implicit req: Request[_]): SendEmailResult = {
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

  def saveVerificationRecord(userId: String, email: String, accType: String)(implicit ec: ExC): Future[Option[Verification]] = {
    val record = Verification(
      verificationId = s"verify-${UUID.randomUUID().toString}",
      userId,
      "email",
      email,
      code = None,
      accType,
      createdAt = new DateTime()
    )
    verificationStore.createVerificationRecord(record) map {
      case MongoSuccessCreate => Some(record)
      case MongoFailedCreate  => None
    }
  }

  def validateVerificationRecord(record: Verification): Future[Option[Verification]] = {
    val query = and(
      equal("verificationId", record.verificationId),
      equal("userId", record.userId),
      equal("contact", record.contact),
      equal("contactType", record.contactType),
    )
    verificationStore.validateVerificationRecord(query)
  }

  def removeVerificationRecord(verificationId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = equal("verificationId", verificationId)
    verificationStore.deleteVerificationRecord(query)
  }
}
