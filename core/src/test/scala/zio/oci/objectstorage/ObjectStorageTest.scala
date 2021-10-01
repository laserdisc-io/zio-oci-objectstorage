package zio.oci.objectstorage

import com.oracle.bmc.Region
import com.oracle.bmc.auth.{SimpleAuthenticationDetailsProvider, StringPrivateKeySupplier}
import zio.{ZLayer, system}
import zio.blocking.Blocking
import zio.stream.ZTransducer
import zio.test._

object ObjectStorageLiveSpec extends DefaultRunnableSpec {
  val objectStorageSettings = ZLayer
    .fromEffect(
      for {
        userId      <- environment.Live.live(system.envOrElse("OCI_USER_ID", ""))
        tenantId    <- environment.Live.live(system.envOrElse("OCI_TENANT_ID", ""))
        privateKey  <- environment.Live.live(system.envOrElse("OCI_PRIVATE_KEY", ""))
        fingerprint <- environment.Live.live(system.envOrElse("OCI_FINGERPRINT", ""))
      } yield ObjectStorageSettings(
        Region.US_ASHBURN_1,
        ObjectStorageAuth(
          SimpleAuthenticationDetailsProvider
            .builder()
            .userId(userId)
            .tenantId(tenantId)
            .privateKeySupplier(new StringPrivateKeySupplier(privateKey))
            .fingerprint(fingerprint)
            .build()
        )
      )
    )
    .mapError(TestFailure.die)

  val objectStorage = objectStorageSettings.flatMap(x => live(x.get[ObjectStorageSettings]).mapError(TestFailure.die))
  override def spec =
    ObjectStorageSuite.spec("ObjectStorageLiveSpec").provideCustomLayerShared(objectStorage) @@ TestAspect.ifEnvSet("OCI_PRIVATE_KEY")
}

object ObjectStorageSuite {
  private val namespace  = "idkqeinxdlui"
  private val bucketName = "zio-oci-objectstorage"

  def spec(label: String): Spec[ObjectStorage with Blocking, TestFailure[Exception], TestSuccess] =
    suite(label)(
      testM("listAllObjects") {
        for {
          list <- listAllObjects(namespace, bucketName).runCollect
        } yield assert(list.map(_.getName()))(Assertion.hasSameElements(List("first", "second", "third")))
      },
      testM("getObject") {
        for {
          o <- getObject(namespace, bucketName, "first").transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(o.size)(Assertion.equalTo(4))
      }
    )
}
