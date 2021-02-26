# ZIO OCI Object Storage
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

