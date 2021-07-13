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

import database.VerificationStore
import dev.cjww.mongo.responses.{MongoDeleteResponse, MongoFailedCreate, MongoSuccessCreate}
import models.{EmailResponse, Verification}
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.mvc.Request

import java.util.UUID
import scala.concurrent.{Future, ExecutionContext => ExC}

trait EmailService {

  val emailSenderAddress: String
  val verificationSubjectLine: String

  val verificationStore: VerificationStore

  def sendEmailVerificationMessage(to: String, record: Verification)(implicit req: Request[_], ec: ExC): Future[EmailResponse]

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
      case MongoFailedCreate => None
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
