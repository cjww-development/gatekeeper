/*
 * Copyright 2019 CJWW Development
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

import com.cjwwdev.featuremanagement.services.{DefaultFeatureService, FeatureService}
import controllers.{ApplicationsController, DefaultApplicationsController, DefaultUserController, UserController}
import database._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services.{ApplicationService, AuthService, DefaultApplicationService, DefaultAuthService, DefaultUserService, UserService}

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    bindGlobals() ++ bindDatabase() ++ bindServices() ++ bindControllers()
  }

  private def bindGlobals(): Seq[Binding[_]] = Seq(
    bind[Indexer].toSelf.eagerly(),
    bind[FeatureService].to[DefaultFeatureService].eagerly(),
  )

  private def bindDatabase(): Seq[Binding[_]] = Seq(
    bind[RegisteredApplicationsStore].to[DefaultRegisteredApplicationsStore].eagerly(),
    bind[UserStore].to[DefaultUserStore].eagerly(),
    bind[AuthStore].to[DefaultAuthStore].eagerly()
  )

  private def bindServices(): Seq[Binding[_]] = Seq(
    bind[ApplicationService].to[DefaultApplicationService].eagerly(),
    bind[UserService].to[DefaultUserService].eagerly(),
    bind[AuthService].to[DefaultAuthService].eagerly()
  )

  private def bindControllers(): Seq[Binding[_]] = Seq(
    bind[ApplicationsController].to[DefaultApplicationsController].eagerly(),
    bind[UserController].to[DefaultUserController].eagerly()
  )
}
