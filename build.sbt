lazy val scala_212 = "2.12.15"
lazy val scala_213 = "2.13.8"

lazy val V = new {
  val ociSdk                = "2.29.0"
  val scalaCollectionCompat = "2.7.0"
  val zio                   = "1.0.14"
  val `zio-nio`             = "1.0.0-RC11"
}

lazy val D = new {
  val objectStorage = Seq(
    "com.oracle.oci.sdk" % "oci-java-sdk-objectstorage" % V.ociSdk
  )

  val scalaModules = Seq(
    "org.scala-lang.modules" %% "scala-collection-compat" % V.scalaCollectionCompat
  )

  val zio = Seq(
    "dev.zio" %% "zio"         % V.zio,
    "dev.zio" %% "zio-streams" % V.zio
  )

  val `zio-nio` = "dev.zio" %% "zio-nio" % V.`zio-nio`

  val zioTest = Seq(
    "dev.zio" %% "zio-test"     % V.zio,
    "dev.zio" %% "zio-test-sbt" % V.zio
  )
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
  organization       := "io.laserdisc",
  scalaVersion       := scala_213,
  crossScalaVersions := Seq(scala_212, scala_213),
  scalacOptions ++= versionDependent(scalaVersion.value),
  homepage          := Some(url("https://github.com/laserdisc-io/zio-oci-objectstorage")),
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT"),
  developers += Developer("amir", "Amir Saeid", "amir@glgdgt.com", url("https://github.com/amir"))
)

lazy val testkit = project
  .in(file("testkit"))
  .settings(commonSettings)
  .settings(
    name := "zio-oci-objectstorage-testkit",
    libraryDependencies ++= D.zio ++ D.objectStorage ++ D.zioTest.map(_ % Test) :+ D.`zio-nio`,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .dependsOn(`zio-oci-objectstorage`)

lazy val `zio-oci-objectstorage` = project
  .in(file("core"))
  .configs(IntegrationTest)
  .settings(commonSettings)
  .settings(Defaults.itSettings)
  .settings(
    name := "zio-oci-objectstorage",
    libraryDependencies ++= D.zio ++ D.objectStorage ++ D.scalaModules ++ D.zioTest.map(_ % "it,test"),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val root = project
  .in(file("."))
  .aggregate(`zio-oci-objectstorage`, testkit)
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    addCommandAlias("fmtCheck", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("fmt", ";test:scalafmtAll;scalafmtAll;scalafmtSbt;test:scalafmtAll"),
    addCommandAlias("fullTest", ";clean;test;IntegrationTest / test"),
    addCommandAlias(
      "setReleaseOptions",
      "set scalacOptions ++= Seq(\"-opt:l:method\", \"-opt:l:inline\", \"-opt-inline-from:laserdisc.**\", \"-opt-inline-from:<sources>\")"
    ),
    addCommandAlias("releaseIt", ";clean;setReleaseOptions;session list;compile;ci-release")
  )
