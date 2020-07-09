/*
 *  Copyright 2019 CJWW Development
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import com.typesafe.config.ConfigFactory

import scala.util.Try

val appName = "gatekeeper"

val btVersion: String = Try(ConfigFactory.load.getString("version")).getOrElse("0.1.0")

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .settings(PlayKeys.playDefaultPort := 5678)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    name                                          := "gatekeeper",
    version                                       :=  btVersion,
    scalaVersion                                  :=  "2.13.2",
    organization                                  :=  "com.cjww-dev.apps",
    resolvers                                     +=  "cjww-dev" at "https://dl.bintray.com/cjww-development/releases",
    libraryDependencies                           ++= Seq(
      "com.cjww-dev.libs"      %  "mongo-connector_2.13"     % "0.2.0",
      "com.cjww-dev.libs"      %  "log-encoding_2.13"        % "0.3.0",
      "com.cjww-dev.libs"      %  "data-defender_2.13"       % "0.5.0",
      "com.cjww-dev.libs"      %  "service-health_2.13"      % "1.1.0",
      "com.cjww-dev.libs"      %  "feature-management_2.13"  % "2.0.1",
      "com.cjww-dev.libs"      %  "inbound-outbound_2.13"    % "0.5.0",
      "com.cjww-dev.libs"      %  "bouncer_2.13"             % "0.2.1",
      "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"  % Test,
      "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"  % IntegrationTest
    ),
    fork                       in IntegrationTest :=  false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution          in IntegrationTest :=  false,
    fork                       in Test            :=  true,
    testForkedParallel         in Test            :=  true,
    parallelExecution          in Test            :=  true,
    logBuffered                in Test            :=  false
  )
