import com.typesafe.config.ConfigFactory

import scala.util.Try
import scoverage.ScoverageKeys

val appName = "gatekeeper"

val btVersion: String = Try(ConfigFactory.load.getString("version")).getOrElse("0.1.0-local")

val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;models/.data/..*;views.*;models.*;global.*;utils.*;forms.*;filters.*;errors.*;database.registries.*;controllers.test.*;controllers.features.*;controllers.shuttering.*;controllers.system.*;common.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimumStmtTotal  := 80,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .settings(scoverageSettings)
  .settings(PlayKeys.playDefaultPort := 5678)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    version :=  btVersion,
    scalaVersion :=  "2.13.6",
    routesImport := Seq.empty,
    semanticdbEnabled :=  true,
    semanticdbVersion :=  scalafixSemanticdb.revision,
    organization :=  "dev.cjww.apps",
    githubTokenSource := (if (Try(ConfigFactory.load.getBoolean("local")).getOrElse(true)) {
      TokenSource.GitConfig("github.token")
    } else {
      TokenSource.Environment("GITHUB_TOKEN")
    }),
    githubOwner :=  "cjww-development",
    githubRepository :=  appName,
    resolvers +=  Resolver.githubPackages("cjww-development"),
    libraryDependencies ++= Seq(
      ws,
      "dev.cjww.libs"                %  "mongo-connector_2.13"       % "1.0.0",
      "dev.cjww.libs"                %  "data-defender_2.13"         % "1.0.0",
      "dev.cjww.libs"                %  "log-encoding_2.13"          % "1.0.0",
      "dev.cjww.libs"                %  "feature-management_2.13"    % "3.0.0",
      "dev.cjww.libs"                %  "inbound-outbound_2.13"      % "1.0.0",
      "io.github.nremond"            %  "pbkdf2-scala_2.13"          % "0.6.5",
      "com.pauldijou"                %  "jwt-core_2.13"              % "5.0.0",
      "com.nimbusds"                 %  "nimbus-jose-jwt"            % "9.23",
      "dev.samstevens.totp"          %  "totp"                       % "1.7.1",
      "com.amazonaws"                %  "aws-java-sdk-ses"           % "1.12.241",
      "com.amazonaws"                %  "aws-java-sdk-sns"           % "1.12.241",
      "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.13.3",
      "org.mockito"                  %  "mockito-core"               % "4.6.1"       % Test,
      "org.scalatestplus"            %  "scalatestplus-mockito_2.13" % "1.0.0-M2"    % Test,
      "org.scalatestplus.play"       %  "scalatestplus-play_2.13"    % "5.1.0"       % Test,
      "org.scalatestplus.play"       %  "scalatestplus-play_2.13"    % "5.1.0"       % IntegrationTest,
      "org.jsoup"                    % "jsoup"                       % "1.15.1"      % IntegrationTest
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
    ),
    Test / testOptions += Tests.Argument("-oF"),
    IntegrationTest / fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    IntegrationTest / parallelExecution :=  false,
    IntegrationTest / logBuffered := true,
    Test / fork := true,
    Test / testForkedParallel := true,
    Test / parallelExecution := true,
    Test / logBuffered := true
  )
      