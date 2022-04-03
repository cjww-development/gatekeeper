/*
 * Copyright 2022 CJWW Development
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

package global

import controllers.api.{ConfigController, DefaultConfigController, DefaultJwksController, DefaultRevokationController, JwksController, RevokationController, AccountController => ApiAccountController, DefaultAccountController => DefaultApiAccountController}
import controllers.features.DefaultFeatureController
import controllers.shuttering.DefaultShutteringController
import controllers.system.{DefaultHealthController, HealthController}
import controllers.test.{DefaultEmailViewTestController, DefaultExceptionTestController, EmailViewTestController, ExceptionTestController}
import controllers.ui._
import database._
import dev.cjww.featuremanagement.controllers.FeatureController
import dev.cjww.featuremanagement.models.Features
import dev.cjww.shuttering.controllers.ShutteringController
import filters.{DefaultRequestLoggingFilter, DefaultShutteringFilter, RequestLoggingFilter}
import orchestrators._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services.comms.email.{DefaultMailgunService, DefaultSesService, EmailService}
import services.comms.{DefaultPhoneService, PhoneService}
import services.oauth2._
import services.security.{DefaultTOTPService, TOTPService}
import services.users._

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    globals() ++
    filters() ++
    dataStores() ++
    serviceLayer() ++
    orchestrators() ++
    controllers() ++
    apiControllers() ++
    testControllers() ++
    systemControllers() ++
    emailService(configuration)
  }

  private def globals(): Seq[Binding[_]] = Seq(
    bind[Features].to[FeatureDef].eagerly(),
    bind[GatekeeperIndexer].toSelf.eagerly()
  )

  private def filters(): Seq[Binding[_]] = Seq(
    bind[DefaultShutteringFilter].toSelf.eagerly(),
    bind[RequestLoggingFilter].to[DefaultRequestLoggingFilter].eagerly()
  )

  private def dataStores(): Seq[Binding[_]] = Seq(
    bind[UserStore].qualifiedWith("individualUserStore").to[IndividualUserStore].eagerly(),
    bind[UserStore].qualifiedWith("organisationUserStore").to[OrganisationUserStore].eagerly(),
    bind[AppStore].to[DefaultAppStore].eagerly(),
    bind[GrantStore].to[DefaultGrantStore].eagerly(),
    bind[LoginAttemptStore].to[DefaultLoginAttemptStore].eagerly(),
    bind[TokenRecordStore].to[DefaultTokenRecordStore].eagerly(),
    bind[VerificationStore].to[DefaultVerificationStore].eagerly(),
    bind[JwksStore].to[DefaultJwksStore].eagerly()
  )

  private def serviceLayer(): Seq[Binding[_]] = Seq(
    bind[RegistrationService].to[DefaultRegistrationService].eagerly(),
    bind[LoginService].to[DefaultLoginService].eagerly(),
    bind[UserService].to[DefaultUserService].eagerly(),
    bind[ScopeService].to[DefaultScopeService].eagerly(),
    bind[GrantService].to[DefaultGrantService].eagerly(),
    bind[TokenService].to[DefaultTokenService].eagerly(),
    bind[ClientService].to[DefaultClientService].eagerly(),
    bind[TOTPService].to[DefaultTOTPService].eagerly(),
    bind[PhoneService].to[DefaultPhoneService].eagerly(),
    bind[JwksService].to[DefaultJwksService].eagerly()
  )

  private def orchestrators(): Seq[Binding[_]] = Seq(
    bind[RegistrationOrchestrator].to[DefaultRegistrationOrchestrator].eagerly(),
    bind[LoginOrchestrator].to[DefaultLoginOrchestrator].eagerly(),
    bind[UserOrchestrator].to[DefaultUserOrchestrator].eagerly(),
    bind[GrantOrchestrator].to[DefaultGrantOrchestrator].eagerly(),
    bind[TokenOrchestrator].to[DefaultTokenOrchestrator].eagerly(),
    bind[WellKnownConfigOrchestrator].to[DefaultWellKnownConfigOrchestrator].eagerly(),
    bind[ClientOrchestrator].to[DefaultClientOrchestrator].eagerly(),
    bind[MFAOrchestrator].to[DefaultMFAOrchestrator].eagerly()
  )

  private def controllers(): Seq[Binding[_]] = Seq(
    bind[RegistrationController].to[DefaultRegistrationController].eagerly(),
    bind[LoginController].to[DefaultLoginController].eagerly(),
    bind[OAuthController].to[DefaultOAuthController].eagerly(),
    bind[FeatureController].to[DefaultFeatureController].eagerly(),
    bind[ShutteringController].to[DefaultShutteringController].eagerly(),
    bind[AccountController].to[DefaultAccountController].eagerly(),
    bind[ClientController].to[DefaultClientController].eagerly(),
    bind[EmailController].to[DefaultEmailController].eagerly(),
    bind[PhoneController].to[DefaultPhoneController].eagerly()
  )

  private def apiControllers(): Seq[Binding[_]] = Seq(
    bind[ApiAccountController].to[DefaultApiAccountController].eagerly(),
    bind[ConfigController].to[DefaultConfigController].eagerly(),
    bind[RevokationController].to[DefaultRevokationController].eagerly(),
    bind[JwksController].to[DefaultJwksController].eagerly()
  )

  private def systemControllers(): Seq[Binding[_]] = Seq(
    bind[HealthController].to[DefaultHealthController].eagerly()
  )

  private def testControllers(): Seq[Binding[_]] = Seq(
    bind[EmailViewTestController].to[DefaultEmailViewTestController].eagerly(),
    bind[ExceptionTestController].to[DefaultExceptionTestController].eagerly()
  )

  private def emailService(config: Configuration): Seq[Binding[_]] = {
    config.get[String]("email-service.selected-provider") match {
      case "ses"     => Seq(bind[EmailService].to[DefaultSesService].eagerly())
      case "mailgun" => Seq(bind[EmailService].to[DefaultMailgunService].eagerly())
      case _         => throw new RuntimeException("Invalid email provider")
    }
  }
}
