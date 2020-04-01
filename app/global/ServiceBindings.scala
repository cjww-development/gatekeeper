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
import controllers.features.DefaultFeatureController
import controllers.shuttering.DefaultShutteringController
import controllers.{ClientController, DefaultClientController, DefaultOAuthController, DefaultRegistrationController, OAuthController, RegistrationController}
import database.{DefaultIndividualUserStore, DefaultOrganisationUserStore, IndividualUserStore, OrganisationUserStore}
import orchestrators.{DefaultRegistrationOrchestrator, RegistrationOrchestrator}
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import services.{DefaultRegistrationService, RegistrationService}

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    globals() ++ dataStores() ++ serviceLayer() ++ orchestrators() ++ controllers()
  }

  private def globals(): Seq[Binding[_]] = Seq(
    bind[Features].to[FeatureDef].eagerly(),
    bind[GatekeeperIndexer].toSelf.eagerly()
  )

  private def dataStores(): Seq[Binding[_]] = Seq(
    bind[IndividualUserStore].to[DefaultIndividualUserStore].eagerly(),
    bind[OrganisationUserStore].to[DefaultOrganisationUserStore].eagerly()
  )

  private def serviceLayer(): Seq[Binding[_]] = Seq(
    bind[RegistrationService].to[DefaultRegistrationService].eagerly()
  )

  private def orchestrators(): Seq[Binding[_]] = Seq(
    bind[RegistrationOrchestrator].to[DefaultRegistrationOrchestrator].eagerly()
  )

  private def controllers(): Seq[Binding[_]] = Seq(
    bind[ClientController].to[DefaultClientController].eagerly(),
    bind[RegistrationController].to[DefaultRegistrationController].eagerly(),
    bind[OAuthController].to[DefaultOAuthController].eagerly(),
    bind[FeatureController].to[DefaultFeatureController].eagerly(),
    bind[ShutteringController].to[DefaultShutteringController].eagerly()
  )
}
