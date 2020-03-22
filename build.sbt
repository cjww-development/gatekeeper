import com.typesafe.config.ConfigFactory
import scala.util.Try

val appName = "gatekeeper"

val btVersion: String = Try(ConfigFactory.load().getString("version")).getOrElse("0.1.0")

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .settings(PlayKeys.playDefaultPort := 5678)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    version                                       :=  btVersion,
    scalaVersion                                  :=  "2.13.1",
    organization                                  :=  "com.cjww-dev.apps",
    resolvers                                     +=  "cjww-dev" at "https://dl.bintray.com/cjww-development/releases",
    libraryDependencies                           ++= Seq(
      "com.cjww-dev.libs"      %  "mongo-connector_2.13" % "0.2.0",
      "com.cjww-dev.libs"      %  "data-defender_2.13"   % "0.5.0",
      "org.scalatestplus.play" %% "scalatestplus-play"   % "5.0.0" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play"   % "5.0.0" % IntegrationTest
    ),
    fork                       in IntegrationTest :=  false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution          in IntegrationTest :=  false,
    fork                       in Test            :=  true,
    testForkedParallel         in Test            :=  true,
    parallelExecution          in Test            :=  true,
    logBuffered                in Test            :=  false
  )
      