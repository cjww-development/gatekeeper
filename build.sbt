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
import com.typesafe.sbt.packager.docker.Cmd
import scoverage.ScoverageKeys

import scala.util.{Failure, Success, Try}

val appName = "gatekeeper"

val btVersion: String = Try(ConfigFactory.load.getString("version")) match {
  case Success(ver) => ver
  case Failure(_)   => "0.1.0"
}

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;/.data/..*;views.*;models.*;global.*;common.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum          := 80,
  ScoverageKeys.coverageFailOnMinimum    := false,
  ScoverageKeys.coverageHighlighting     := true
)

name := "gatekeeper"
 
version := "1.0" 

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .settings(scoverageSettings : _*)
  .configs(IntegrationTest)
  .settings(PlayKeys.playDefaultPort := 5678)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    version                                       :=  btVersion,
    scalaVersion                                  :=  "2.12.8",
    organization                                  :=  "com.cjww-dev.apps",
    resolvers                                     +=  "cjww-dev" at "http://dl.bintray.com/cjww-development/releases",
    libraryDependencies                           ++= AppDependencies(),
    fork                       in IntegrationTest :=  false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution          in IntegrationTest :=  false,
    fork                       in Test            :=  true,
    testForkedParallel         in Test            :=  true,
    parallelExecution          in Test            :=  true,
    logBuffered                in Test            :=  false,
    dockerRepository                              :=  Some("cjwwdevelopment"),
    dockerCommands                                :=  Seq(
      Cmd("FROM", "openjdk:8u181-jdk"),
      Cmd("WORKDIR", "/opt/docker"),
      Cmd("ADD", "--chown=daemon:daemon opt /opt"),
      Cmd("USER", "daemon"),
      Cmd("ENTRYPOINT", """["/opt/docker/bin/default"]"""),
      Cmd("CMD", """[]""")
    )
  )
