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

package global

import com.cjwwdev.featuremanagement.controllers.FeatureController
import com.cjwwdev.featuremanagement.models.Features
import com.cjwwdev.shuttering.controllers.ShutteringController
import controllers.api.{ConfigController, DefaultConfigController, AccountController => ApiAccountController, DefaultAccountController => DefaultApiAccountController}
import controllers.features.DefaultFeatureController
import controllers.shuttering.DefaultShutteringController
import controllers.ui._
import database._
import filters.DefaultShutteringFilter
import orchestrators._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services._

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    globals() ++ filters() ++ dataStores() ++ serviceLayer() ++ orchestrators() ++ controllers() ++ apiControllers()
  }

  private def globals(): Seq[Binding[_]] = Seq(
    bind[Features].to[FeatureDef].eagerly(),
    bind[GatekeeperIndexer].toSelf.eagerly()
  )

  private def filters(): Seq[Binding[_]] = Seq(
    bind[DefaultShutteringFilter].toSelf.eagerly()
  )

  private def dataStores(): Seq[Binding[_]] = Seq(
    bind[IndividualUserStore].to[DefaultIndividualUserStore].eagerly(),
    bind[OrganisationUserStore].to[DefaultOrganisationUserStore].eagerly(),
    bind[AppStore].to[DefaultAppStore].eagerly(),
    bind[GrantStore].to[DefaultGrantStore].eagerly(),
    bind[LoginAttemptStore].to[DefaultLoginAttemptStore].eagerly()
  )

  private def serviceLayer(): Seq[Binding[_]] = Seq(
    bind[RegistrationService].to[DefaultRegistrationService].eagerly(),
    bind[LoginService].to[DefaultLoginService].eagerly(),
    bind[AccountService].to[DefaultAccountService].eagerly(),
    bind[ScopeService].to[DefaultScopeService].eagerly(),
    bind[GrantService].to[DefaultGrantService].eagerly(),
    bind[TokenService].to[DefaultTokenService].eagerly(),
    bind[ClientService].to[DefaultClientService].eagerly(),
    bind[TOTPService].to[DefaultTOTPService].eagerly()
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
  )

  private def apiControllers(): Seq[Binding[_]] = Seq(
    bind[ApiAccountController].to[DefaultApiAccountController].eagerly(),
    bind[ConfigController].to[DefaultConfigController].eagerly(),
  )
}
