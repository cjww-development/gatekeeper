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

import models.Login
import org.slf4j.LoggerFactory
import services.{LoginService, Secret, TOTPService}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait MFAResponse
case class NoMFAChallengeNeeded(userId: String) extends MFAResponse
case object InvalidLogonAttempt extends MFAResponse
case object TOTPMFAChallenge extends MFAResponse

sealed trait MFAResult
case class MFAValidated(userId: String) extends MFAResult
case object MFAInvalid extends MFAResult
case object MFAError extends MFAResult


class DefaultLoginOrchestrator @Inject()(val loginService: LoginService,
                                         val totpService: TOTPService) extends LoginOrchestrator

trait LoginOrchestrator {

  protected val loginService: LoginService
  protected val totpService: TOTPService

  private val logger = LoggerFactory.getLogger(this.getClass)

  def authenticateUser(loginAttempt: Login)(implicit ec: ExC): Future[Option[String]] = {
    loginService.getUserSalt(loginAttempt.accountId).flatMap {
      case Some(salt) =>
        val hashedLoginAttempt = Login.apply(loginAttempt.accountId, salt, loginAttempt.password)
        for {
          user <- loginService.validateUser(hashedLoginAttempt.accountId, hashedLoginAttempt.password)
          attemptId <- user.fold[Future[Option[String]]](Future.successful(None)) {
            user => loginService.saveLoginAttempt(user.id, successfulAttempt = true)
          }
        } yield attemptId
      case None =>
        logger.warn("[authenticateUser] - No salt found for user, aborting login attempt")
        Future.successful(None)
    }
  }

  def mfaChallengePresenter(attemptId: String)(implicit ec: ExC): Future[MFAResponse] = {
    loginService.lookupLoginAttempt(attemptId) flatMap {
      case Some(userId) => totpService.getMFAStatus(userId) map { shouldChallenge =>
        if(shouldChallenge) {
          logger.info(s"[mfaChallengePresenter] - MFA enabled for user $userId, presenting MFA challenge")
          TOTPMFAChallenge
        } else {
          NoMFAChallengeNeeded(userId)
        }
      }
      case None =>
        logger.warn(s"[mfaChallenge] - Could not find valid logon attempt for attempt $attemptId")
        Future.successful(InvalidLogonAttempt)
    }
  }

  def verifyMFAChallenge(attemptId: String, code: String)(implicit ec: ExC): Future[MFAResult] = {
    loginService.lookupLoginAttempt(attemptId) flatMap {
      case Some(userId) =>
        logger.info(s"[verifyMFAChallenge] - Found login attempt matching attempt $attemptId")
        totpService.getCurrentSecret(userId) map {
          case Secret(secret) => if(totpService.validateCodes(secret, code)) {
            logger.info(s"[verifyMFAChallenge] - Successfully validated attempt $attemptId with MFA code")
            MFAValidated(userId)
          } else {
            logger.warn(s"[verifyMFAChallenge] - MFA code for attempt $attemptId was invalid")
            MFAInvalid
          }
          case _ =>
            logger.error(s"[verifyMFAChallenge] - Problem fetching the secret for user $userId")
            MFAError
        }
      case None =>
        logger.error(s"[verifyMFAChallenge] - No matching logon attempt was found for attempt $attemptId")
        Future.successful(MFAError)
    }
  }
}
