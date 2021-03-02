lazy val scala_212 = "2.13.5"
lazy val scala_213 = "2.13.5"

lazy val V = new {
  val ociSdk = "1.32.1"
  val zio    = "1.0.4-2"
}

lazy val D = new {
  val objectStorage = Seq(
    "com.oracle.oci.sdk" % "oci-java-sdk-objectstorage" % V.ociSdk
  )

  val zio = Seq(
    "dev.zio" %% "zio"         % V.zio,
    "dev.zio" %% "zio-streams" % V.zio
  )

  val zioTest = Seq(
    "dev.zio" %% "zio-test"     % V.zio,
    "dev.zio" %% "zio-test-sbt" % V.zio
  ).map(_ % Test)
}

lazy val flags = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-language:implicitConversions",
  "-unchecked",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfatal-warnings",
  "-Ywarn-unused",
  "-opt-warnings",
  "-Xlint:constant",
  "-Ywarn-extra-implicit"
)

def versionDependent(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 13 =>
      flags ++ Seq(
        "-Wconf:any:error",
        "-Ymacro-annotations",
        "-Xlint:-byname-implicit"
      )
    case _ =>
      flags ++ Seq(
        "-Xfuture",
        "-Xlint:by-name-right-associative",
        "-Xlint:unsound-match",
        "-Yno-adapted-args",
        "-Ypartial-unification",
        "-Ywarn-inaccessible",
        "-Ywarn-infer-any",
        "-Ywarn-nullary-override",
        "-Ywarn-nullary-unit"
      )
  }

lazy val commonSettings = Seq(
  organization := "io.laserdisc",
  scalaVersion := scala_213,
  crossScalaVersions := Seq(scala_212, scala_213),
  scalacOptions ++= versionDependent(scalaVersion.value),
  homepage := Some(url("https://github.com/laserdisc-io/zio-oci-objectstorage")),
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
)

lazy val `zio-oci-objectstorage` = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    name := "zio-oci-objectstorage",
    libraryDependencies ++= D.zio ++ D.objectStorage ++ D.zioTest,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    addCommandAlias("fmtCheck", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("fmt", ";test:scalafmtAll;scalafmtAll;scalafmtSbt;test:scalafmtAll"),
    addCommandAlias("fullTest", ";clean;test"),
    addCommandAlias(
      "setReleaseOptions",
      "set scalacOptions ++= Seq(\"-opt:l:method\", \"-opt:l:inline\", \"-opt-inline-from:laserdisc.**\", \"-opt-inline-from:<sources>\")"
    ),
    addCommandAlias("releaseIt", ";clean;setReleaseOptions;session list;compile;ci-release")
  )
