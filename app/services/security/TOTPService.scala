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

package services.security

import database.{UserStore, UserStoreUtils}
import dev.cjww.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate}
import dev.samstevens.totp.code.{CodeVerifier, DefaultCodeGenerator, DefaultCodeVerifier, HashingAlgorithm}
import dev.samstevens.totp.qr.{QrData, ZxingPngQrGenerator}
import dev.samstevens.totp.secret.{DefaultSecretGenerator, SecretGenerator}
import dev.samstevens.totp.time.SystemTimeProvider
import org.apache.commons.codec.binary.Base64
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import utils.StringUtils._

import javax.inject.{Inject, Named}
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait SecretResponse
case class Secret(secret: String) extends SecretResponse
case object FailedGeneration extends SecretResponse

sealed trait QRCodeResponse
case class QRCode(qrData: String) extends QRCodeResponse
case object QRCodeFailed extends QRCodeResponse

sealed trait MFAEnabledResponse
case object MFAEnabled extends MFAEnabledResponse
case object MFADisabled extends MFAEnabledResponse

class DefaultTOTPService @Inject()(@Named("individualUserStore") val individualUserStore: UserStore,
                                   @Named("organisationUserStore") val organisationUserStore: UserStore,
                                   val config: Configuration) extends TOTPService {
  override val secretGenerator: SecretGenerator = new DefaultSecretGenerator()
  override val qrGenerator: ZxingPngQrGenerator = new ZxingPngQrGenerator()

  override val algorithm: HashingAlgorithm = HashingAlgorithm.SHA512
  override val mfaIssuer: String = config.get[String]("mfa.totp.issuer")
  override val mfaDigits: Int = config.get[Int]("mfa.totp.digits")
  override val mfaPeriod: Int = config.get[Int]("mfa.totp.period")

  private val codeGenerator = new DefaultCodeGenerator(algorithm)
  private val timeProvider = new SystemTimeProvider()

  override val codeVerifier: CodeVerifier = new DefaultCodeVerifier(
    codeGenerator,
    timeProvider
  )
}

trait TOTPService extends UserStoreUtils {
  val secretGenerator: SecretGenerator
  val qrGenerator: ZxingPngQrGenerator
  val codeVerifier: CodeVerifier
  val algorithm: HashingAlgorithm

  val mfaIssuer: String
  val mfaDigits: Int
  val mfaPeriod: Int

  private val query: String => Bson = userId => equal("id", userId)

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def generateSecret(userId: String)(implicit ec: ExC): Future[SecretResponse] = {
    val secret = secretGenerator.generate()

    getUserStore(userId).updateUser(query(userId), set("mfaSecret", secret)).map {
      case MongoSuccessUpdate =>
        logger.info(s"[generateSecret] - Generated new MFA secret for user $userId")
        Secret(secret)
      case MongoFailedUpdate =>
        logger.error(s"[generateSecret] - There was a problem saving the MFA secret against the user")
        FailedGeneration
    }
  }

  def getCurrentSecret(userId: String)(implicit ec: ExC): Future[SecretResponse] = {
    getUserStore(userId)
      .findUser(query(userId))
      .map(_.map(_.mfaSecret.fold[SecretResponse](FailedGeneration)(sec => Secret(sec))).getOrElse(FailedGeneration))
  }

  def generateQRCode(userId: String, secret: String)(implicit ec: ExC): Future[QRCodeResponse] = {
    getUserStore(userId).findUser(query(userId)).map {
      case Some(user) =>
        val qrData = new QrData.Builder()
          .label(user.userName.decrypt.getOrElse(""))
          .secret(secret)
          .issuer(mfaIssuer)
          .algorithm(algorithm)
          .digits(mfaDigits)
          .period(mfaPeriod)
          .build()

        val imageData: Array[Byte] = qrGenerator.generate(qrData)
        val mimeType = qrGenerator.getImageMimeType
        logger.info(s"[generateQRCode] - Generated QR code for user $userId")
        val encodedData = new String(new Base64().encode(imageData))
        QRCode(String.format("data:%s;base64,%s", mimeType, encodedData))
      case None =>
        logger.warn(s"[generateQRCode] - No user found for $userId, failed to generate a secret for TOTP MFA")
        QRCodeFailed
    }
  }

  def validateCodes(secret: String, codes: String*): Boolean = {
    codes.forall(code => codeVerifier.isValidCode(secret, code))
  }

  def enableAccountMFA(userId: String)(implicit ec: ExC): Future[MFAEnabledResponse] = {
    getUserStore(userId).updateUser(query(userId), set("mfaEnabled", true)) map {
      case MongoSuccessUpdate =>
        logger.info(s"[enableAccountMFA] - TOTP MFA enabled for user $userId")
        MFAEnabled
      case MongoFailedUpdate =>
        logger.warn(s"[enableAccountMFA] - There was a problem enabling TOTP MFA for user $userId")
        MFADisabled
    }
  }

  def getMFAStatus(userId: String)(implicit ec: ExC): Future[Boolean] = {
    getUserStore(userId).findUser(query(userId)).map(_.exists(_.mfaEnabled))
  }

  def removeTOTPMFA(userId: String)(implicit ec: ExC): Future[Boolean] = {
    val update = combine(set("mfaEnabled", false), unset("mfaSecret"))

    getUserStore(userId).updateUser(query(userId), update).map {
      case MongoSuccessUpdate =>
        logger.info(s"[removeTOTPMFA] - Removed TOTP MFA for user $userId")
        true
      case MongoFailedUpdate =>
        logger.error(s"[removeTOTPMFA] - There was a problem removing TOTP MFA for user $userId")
        false
    }
  }
}


