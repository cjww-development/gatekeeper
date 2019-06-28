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

import sbt._
import play.sbt.PlayImport._

object AppDependencies {
  def apply(): Seq[ModuleID] = CompileDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

private object CompileDependencies {
  private val playImports: Seq[ModuleID] = Seq(filters)

  private val externalDependencies: Seq[ModuleID] = Seq(
    "mysql"             % "mysql-connector-java"       % "8.0.16",
    "com.typesafe.play" % "play-slick_2.12"            % "3.0.3",
    "com.typesafe.play" % "play-slick-evolutions_2.12" % "3.0.3",
    "com.cjww-dev.libs" % "authorisation_2.12"         % "4.9.0",
    "com.cjww-dev.libs" % "application-utilities_2.12" % "4.6.1",
    "com.cjww-dev.libs" % "service-health_2.12"        % "0.3.1",
    "com.cjww-dev.libs" % "feature-management_2.12"    % "1.5.0",
    "com.cjww-dev.libs" % "logging-utils_2.12"         % "1.3.1",
    "com.cjww-dev.libs" % "metrics-reporter_2.12"      % "1.0.3"
  )

  def apply(): Seq[ModuleID] = externalDependencies ++ playImports
}

private object UnitTestDependencies {
  private val testDependencies: Seq[ModuleID] = Seq(
    "com.cjww-dev.libs" % "testing-framework_2.12" % "3.2.0" % Test
  )

  def apply(): Seq[ModuleID] = testDependencies
}

private object IntegrationTestDependencies {
  private val testDependencies: Seq[ModuleID] = Seq(
    "com.cjww-dev.libs" % "testing-framework_2.12" % "3.2.0" % IntegrationTest
  )

  def apply(): Seq[ModuleID] = testDependencies
}
