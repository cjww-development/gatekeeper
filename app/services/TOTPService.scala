package services

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import com.cjwwdev.security.deobfuscation.DeObfuscators
import database.{IndividualUserStore, OrganisationUserStore}
import dev.samstevens.totp.code.{CodeVerifier, DefaultCodeGenerator, DefaultCodeVerifier, HashingAlgorithm}
import dev.samstevens.totp.qr.{QrData, ZxingPngQrGenerator}
import dev.samstevens.totp.secret.{DefaultSecretGenerator, SecretGenerator}
import dev.samstevens.totp.time.SystemTimeProvider
import dev.samstevens.totp.util.Utils.getDataUriForImage
import javax.inject.Inject
import models.User
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import play.api.Configuration

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

class DefaultTOTPService @Inject()(val individualUserStore: IndividualUserStore,
                                   val organisationUserStore: OrganisationUserStore,
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

trait TOTPService extends DeObfuscators {
  override val locale: String = ""

  val secretGenerator: SecretGenerator
  val qrGenerator: ZxingPngQrGenerator
  val codeVerifier: CodeVerifier
  val algorithm: HashingAlgorithm

  val individualUserStore: IndividualUserStore
  val organisationUserStore: OrganisationUserStore

  val mfaIssuer: String
  val mfaDigits: Int
  val mfaPeriod: Int

  private val getUser: String => Future[Option[User]] = {
    case userId@x if x.startsWith("user-") => individualUserStore.validateUserOn(equal("id", userId))
    case userId@x if x.startsWith("org-user-") => organisationUserStore.validateUserOn(equal("id", userId))
  }

  private def updateUser[T](userId: String, key: String, data: T)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    userId match {
      case x if x.startsWith("user-") => individualUserStore.updateUser(equal("id", userId), set(key, data))
      case x if x.startsWith("org-user-") => organisationUserStore.updateUser(equal("id", userId), set(key, data))
    }
  }

  def generateSecret(userId: String)(implicit ec: ExC): Future[SecretResponse] = {
    val secret = secretGenerator.generate()
    val updateResp = updateUser[String](userId, "mfaSecret", secret)

    updateResp.map {
      case MongoSuccessUpdate =>
        logger.info(s"[generateSecret] - Generated new MFA secret for user $userId")
        Secret(secret)
      case MongoFailedUpdate =>
        logger.error(s"[generateSecret] - There was a problem saving the MFA secret against the user")
        FailedGeneration
    }
  }

  def getCurrentSecret(userId: String)(implicit ec: ExC): Future[SecretResponse] = {
    getUser(userId)
      .map(_.map(_.mfaSecret.fold[SecretResponse](FailedGeneration)(sec => Secret(sec))).getOrElse(FailedGeneration))
  }

  def generateQRCode(userId: String, secret: String)(implicit ec: ExC): Future[QRCodeResponse] = {
    getUser(userId).map {
      case Some(user) =>
        val qrData = new QrData.Builder()
          .label(stringDeObfuscate.decrypt(user.userName).getOrElse(""))
          .secret(secret)
          .issuer(mfaIssuer)
          .algorithm(algorithm)
          .digits(mfaDigits)
          .period(mfaPeriod)
          .build()

        val imageData: Array[Byte] = qrGenerator.generate(qrData)
        val mimeType = qrGenerator.getImageMimeType
        logger.info(s"[generateQRCode] - Generated QR code for user $userId")
        QRCode(getDataUriForImage(imageData, mimeType))
      case None =>
        logger.warn(s"[generateQRCode] - No user found for $userId, failed to generate a secret for TOTP MFA")
        QRCodeFailed
    }
  }

  def validateCodes(secret: String, codes: String*): Boolean = {
    codes.forall(code => codeVerifier.isValidCode(secret, code))
  }

  def enableAccountMFA(userId: String)(implicit ec: ExC): Future[MFAEnabledResponse] = {
    val userInfo = updateUser[Boolean](userId, "mfaEnabled", true)

    userInfo map {
      case MongoSuccessUpdate =>
        logger.info(s"[enableAccountMFA] - TOTP MFA enabled for user $userId")
        MFAEnabled
      case MongoFailedUpdate =>
        logger.warn(s"[enableAccountMFA] - There was a problem enabling TOTP MFA for user $userId")
        MFADisabled
    }
  }

  def getMFAStatus(userId: String)(implicit ec: ExC): Future[Boolean] = {
    getUser(userId).map(_.exists(_.mfaEnabled))
  }
}


