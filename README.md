# ZIO OCI Object Storage
[![CI](https://github.com/laserdisc-io/zio-oci-objectstorage/workflows/CI/badge.svg?branch=master)](https://github.com/laserdisc-io/zio-oci-objectstorage/actions?query=workflow%3ACI+branch%3Amaster)
[![Release](https://github.com/laserdisc-io/zio-oci-objectstorage/workflows/Release/badge.svg)](https://github.com/laserdisc-io/zio-oci-objectstorage/actions?query=workflow%3ARelease)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.laserdisc/zio-oci-objectstorage_2.13/badge.svg?kill_cache=1&color=orange)](https://search.maven.org/artifact/io.laserdisc/zio-oci-objectstorage_2.13/)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

An OCI Object Storage client for ZIO

Usage
-----

```scala
import com.oracle.bmc.Region
import zio.ExitCode
import zio.console._
import zio.stream.ZSink

object App extends zio.App {
  val mkObjectStorageLayer = ObjectStorageAuth.fromConfigFileDefaultProfile.map(auth => live(ObjectStorageSettings(Region.US_ASHBURN_1, auth)))

  val program = for {
    _ <- getObject(
      "namespace",
      "bucket",
      "object"
    ).run(ZSink.fromOutputStream(new java.io.FileOutputStream("/tmp/foo")))
  } yield ()

  override def run(args: List[String]): zio.URIO[zio.ZEnv, ExitCode] =
    mkObjectStorageLayer.flatMap(osLayer => program.provideCustomLayer(osLayer)).exitCode
}

```

