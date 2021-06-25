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
import com.amazonaws.services.sns.model.{MessageAttributeValue, PublishRequest, PublishResult}
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClientBuilder}
import database.VerificationStore
import dev.cjww.mongo.responses.MongoDeleteResponse
import models.Verification
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.Configuration

import java.util
import java.util.{Random, UUID}
import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultPhoneService @Inject()(val config: Configuration,
                                    val verificationStore: VerificationStore) extends PhoneService {
  override protected val verifyMessage: String = config.get[String]("sms.verification.message")
  override protected val verifySenderId: String = config.get[String]("sms.verification.sender-id")
  override protected val maxPrice: String = config.get[String]("sms.max-price")
  override protected val verifyMsgType: String = config.get[String]("sms.verification.type")
  override protected val snsClient: AmazonSNS = AmazonSNSClientBuilder
    .standard()
    .withRegion(Regions.EU_WEST_1)
    .build()
}

trait PhoneService {

  protected val snsClient: AmazonSNS
  protected val verifyMessage: String
  protected val verifySenderId: String
  protected val verifyMsgType: String
  protected val maxPrice: String

  protected val verificationStore: VerificationStore

  def sendSMSVerification(phoneNumber: String, code: String): PublishResult = {
    val msgAttrs = new util.HashMap[String, MessageAttributeValue]
    msgAttrs.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue().withStringValue(verifySenderId).withDataType("String"))
    msgAttrs.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue().withStringValue(maxPrice).withDataType("Number"))
    msgAttrs.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue().withStringValue(verifyMsgType).withDataType("String"))

    snsClient.publish(new PublishRequest()
      .withMessage(verifyMessage.replace("<replace>", code))
      .withPhoneNumber(phoneNumber)
      .withMessageAttributes(msgAttrs))
  }

  def saveVerificationRecord(userId: String, phoneNumber: String, accType: String)(implicit ec: ExC): Future[Verification] = {
    val ran = new Random()
    val record = Verification(
      verificationId = s"verify-${UUID.randomUUID().toString}",
      userId,
      "phone",
      phoneNumber,
      code = Some((100000 + ran.nextInt(900000)).toString),
      accType,
      createdAt = new DateTime()
    )
    verificationStore.createVerificationRecord(record) map(_ => record)
  }

  def validateVerificationRecord(userId: String, code: String): Future[Option[Verification]] = {
    val query = and(
      equal("userId", userId),
      equal("code", code),
      equal("contactType", "phone")
    )
    verificationStore.validateVerificationRecord(query)
  }

  def removeVerificationRecord(verificationId: String)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = equal("verificationId", verificationId)
    verificationStore.deleteVerificationRecord(query)
  }
}
