import org.typelevel.sbt.gha.WorkflowStep.Sbt

val scala_213 = "2.13.18"
val scala_3   = "3.3.7"

val V = new {
  val ociSdk    = "3.77.2"
  val zio       = "2.1.24"
  val `zio-nio` = "2.0.2"
}

lazy val D = new {
  val objectStorage       = "com.oracle.oci.sdk" % "oci-java-sdk-objectstorage"            % V.ociSdk
  val zio                 = "dev.zio"           %% "zio"                                   % V.zio
  val `zio-nio`           = "dev.zio"           %% "zio-nio"                               % V.`zio-nio`
  val `zio-streams`       = "dev.zio"           %% "zio-streams"                           % V.zio
  val `httpclient-jersey` = "com.oracle.oci.sdk" % "oci-java-sdk-common-httpclient-jersey" % V.ociSdk % Test
  val `zio-test`          = "dev.zio"           %% "zio-test"                              % V.zio    % Test
  val `zio-test-sbt`      = "dev.zio"           %% "zio-test-sbt"                          % V.zio    % Test
}

ThisBuild / tlBaseVersion              := "0.8"
ThisBuild / tlCiReleaseBranches        := Seq("master")
ThisBuild / tlJdkRelease               := Some(11)
ThisBuild / organization               := "io.laserdisc"
ThisBuild / organizationName           := "LaserDisc"
ThisBuild / licenses                   := Seq(License.MIT)
ThisBuild / startYear                  := Some(2021)
ThisBuild / developers                 := List(tlGitHubDev("amir", "Amir Saeid"))
ThisBuild / crossScalaVersions         := Seq(scala_213, scala_3)
ThisBuild / scalaVersion               := scala_213
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"), JavaSpec.temurin("17"), JavaSpec.temurin("21"))

ThisBuild / githubWorkflowEnv += ("OCI_PRIVATE_KEY", "/tmp/oci_key.pem")
ThisBuild / githubWorkflowBuildPreamble += WorkflowStep.Run(
  commands = List("echo $OCI_PRIVATE_KEY_BASE64 | base64 -d > $OCI_PRIVATE_KEY"),
  env = Map(("OCI_PRIVATE_KEY_BASE64", "${{ secrets.OCI_PRIVATE_KEY_BASE64 }}")),
  name = Some("Set up OCI key")
)
ThisBuild / githubWorkflowBuild ~= {
  _.map {
    case sbt: Sbt if sbt.name == Some("Test") =>
      sbt.concatEnv(
        List(
          ("OCI_USER_ID", "${{ secrets.OCI_USER_ID }}"),
          ("OCI_TENANT_ID", "${{ secrets.OCI_TENANT_ID }}"),
          ("OCI_FINGERPRINT", "${{ secrets.OCI_FINGERPRINT }}")
        )
      )
    case other => other
  }
}

lazy val commonSettings = Seq(
  headerEndYear := Some(2025),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, major)) if major >= 13 => Seq("-Wconf:cat=lint-infer-any:s")
      case _                               => Seq.empty
    }
  },
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  Test / fork := true
)

lazy val `zio-oci-objectstorage` = tlCrossRootProject
  .aggregate(core, `integration-tests`, testkit)

lazy val testkit = project
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "zio-oci-objectstorage-testkit",
    libraryDependencies ++= Seq(
      D.objectStorage,
      D.zio,
      D.`zio-nio`,
      D.`zio-streams`,
      D.`zio-test`,
      D.`zio-test-sbt`
    )
  )

lazy val core = project
  .settings(commonSettings)
  .settings(
    name := "zio-oci-objectstorage",
    libraryDependencies ++= Seq(
      D.objectStorage,
      D.`httpclient-jersey`,
      D.zio,
      D.`zio-streams`,
      D.`zio-test`,
      D.`zio-test-sbt`
    )
  )

lazy val `integration-tests` = project
  .in(file("it"))
  .dependsOn(core)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .settings(
    name := "zio-oci-objectstorage-it",
    libraryDependencies ++= Seq(
      D.objectStorage,
      D.`httpclient-jersey`,
      D.zio,
      D.`zio-streams`,
      D.`zio-test`,
      D.`zio-test-sbt`
    )
  )
