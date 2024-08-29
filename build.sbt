lazy val V = new {
  val ociSdk    = "3.48.0"
  val scala213  = "2.13.14"
  val `zio-nio` = "2.0.2"
}

lazy val D = new {
  val objectStorage       = "com.oracle.oci.sdk" % "oci-java-sdk-objectstorage"            % V.ociSdk
  val `zio-nio`           = "dev.zio"           %% "zio-nio"                               % V.`zio-nio`
  val `httpclient-jersey` = "com.oracle.oci.sdk" % "oci-java-sdk-common-httpclient-jersey" % V.ociSdk % Test
}

enablePlugins(ZioSbtEcosystemPlugin)

inThisBuild(
  Seq(
    name               := "ZIO OCI ObjectStorage",
    zioVersion         := "2.1.9",
    organization       := "io.laserdisc",
    scalaVersion       := V.scala213,
    crossScalaVersions := Seq(V.scala213, scala3.value),
    homepage           := Some(url("https://github.com/laserdisc-io/zio-oci-objectstorage")),
    licenses += "MIT"  -> url("http://opensource.org/licenses/MIT"),
    developers += Developer("amir", "Amir Saeid", "amir@glgdgt.com", url("https://github.com/amir"))
  )
)

lazy val testkit = project
  .in(file("testkit"))
  .settings(enableZIO(enableStreaming = true))
  .settings(
    name := "zio-oci-objectstorage-testkit",
    libraryDependencies ++= D.objectStorage :: D.`zio-nio` :: Nil
  )
  .dependsOn(core)

lazy val core = project
  .in(file("core"))
  .settings(enableZIO(enableStreaming = true))
  .settings(
    name := "zio-oci-objectstorage",
    libraryDependencies ++= D.objectStorage :: D.`httpclient-jersey` :: Nil
  )

lazy val `integration-tests` = project
  .in(file("it"))
  .settings(enableZIO(enableStreaming = true))
  .settings(
    name := "zio-oci-objectstorage-it",
    libraryDependencies ++= D.objectStorage :: D.`httpclient-jersey` :: Nil,
    publish / skip := true
  )
  .dependsOn(core)

lazy val root = project
  .in(file("."))
  .aggregate(core, `integration-tests`, testkit)
  .settings(
    publish / skip := true,
    addCommandAlias("fmtCheck", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt;Test/scalafmtAll"),
    addCommandAlias("fullTest", ";clean;test"),
    addCommandAlias(
      "setReleaseOptions",
      "set scalacOptions ++= Seq(\"-opt:l:method\", \"-opt:l:inline\", \"-opt-inline-from:laserdisc.**\", \"-opt-inline-from:<sources>\")"
    ),
    addCommandAlias("releaseIt", ";clean;setReleaseOptions;session list;compile;ci-release")
  )
