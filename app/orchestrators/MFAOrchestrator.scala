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

package orchestrators

import org.slf4j.{Logger, LoggerFactory}
import services._

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait MFAGenerationResponse
case class MFATOTPQR(qrCodeData: String) extends MFAGenerationResponse
case object QRGenerationFailed extends MFAGenerationResponse
case object SecretGenerationFailed extends MFAGenerationResponse

sealed trait MFAVerificationResponse
case object MissingMFASecret extends MFAVerificationResponse
case object ValidCodePair extends MFAVerificationResponse
case object InvalidCodePair extends MFAVerificationResponse
case object MFAInvalidUser extends MFAVerificationResponse
case object FailedToEnable extends MFAVerificationResponse

class DefaultMFAOrchestrator @Inject()(val totpService: TOTPService) extends MFAOrchestrator

trait MFAOrchestrator {

  val totpService: TOTPService

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def setupTOTPMFA(userId: String)(implicit ec: ExC): Future[MFAGenerationResponse] = {
    totpService.generateSecret(userId) flatMap {
      case Secret(secret) => totpService.generateQRCode(userId, secret) map {
        case QRCode(qrData) => MFATOTPQR(qrData)
        case QRCodeFailed => QRGenerationFailed
      }
      case FailedGeneration => Future.successful(SecretGenerationFailed)
    }
  }

  def postTOTPSetupCodeVerification(userId: String, codeOne: String, codeTwo: String)(implicit ec: ExC): Future[MFAVerificationResponse] = {
    totpService.getCurrentSecret(userId) flatMap {
      case Secret(secret) => if(totpService.validateCodes(secret, codeOne, codeTwo)) {
        totpService.enableAccountMFA(userId) map {
          case MFAEnabled => ValidCodePair
          case MFADisabled => FailedToEnable
        }
      } else {
        Future.successful(InvalidCodePair)
      }
      case FailedGeneration => Future.successful(MissingMFASecret)
    }
  }

  def isMFAEnabled(userId: String)(implicit ec: ExC): Future[Boolean] = {
    totpService.getMFAStatus(userId) map { status =>
      if(status) {
        logger.info(s"[isMFAEnabled] - MFA is enabled for user $userId")
      } else {
        logger.info(s"[isMFAEnabled] - MFA is disabled for user $userId")
      }
      status
    }
  }

  def disableMFA(userId: String)(implicit ec: ExC): Future[Boolean] = {
    logger.info(s"[disableMFA] - Attempting to disable MFA for user $userId")
    totpService.removeTOTPMFA(userId)
  }
}
