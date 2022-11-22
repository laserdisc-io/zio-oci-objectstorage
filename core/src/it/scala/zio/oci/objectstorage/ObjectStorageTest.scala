package zio.oci.objectstorage

import com.oracle.bmc.Region
import com.oracle.bmc.auth.{SimpleAuthenticationDetailsProvider, SimplePrivateKeySupplier}
import zio.{Chunk, Layer, System, ZLayer}
import zio.stream.{ZPipeline, ZSink}
import zio.test._

import java.security.MessageDigest

object ObjectStorageLiveSpec extends ZIOSpecDefault {
  val objectStorageSettings = ZLayer
    .fromZIO(
      for {
        userId      <- liveEnvironment(System.SystemLive.envOrElse("OCI_USER_ID", ""))
        tenantId    <- liveEnvironment(System.SystemLive.envOrElse("OCI_TENANT_ID", ""))
        privateKey  <- liveEnvironment(System.SystemLive.envOrElse("OCI_PRIVATE_KEY", ""))
        fingerprint <- liveEnvironment(System.SystemLive.envOrElse("OCI_FINGERPRINT", ""))
      } yield ObjectStorageSettings(
        Region.US_ASHBURN_1,
        ObjectStorageAuth(
          SimpleAuthenticationDetailsProvider
            .builder()
            .userId(userId)
            .tenantId(tenantId)
            .privateKeySupplier(new SimplePrivateKeySupplier(privateKey))
            .fingerprint(fingerprint)
            .build()
        )
      )
    )
    .mapError(TestFailure.die)

  val objectStorage: Layer[TestFailure[Nothing], ObjectStorage] =
    objectStorageSettings.flatMap(x => ObjectStorage.live(x.get).mapError(TestFailure.die))

  override def spec =
    ObjectStorageSuite.spec("ObjectStorageLiveSpec").provideLayerShared(objectStorage) @@ TestAspect.ifEnvSet("OCI_PRIVATE_KEY")
}

object ObjectStorageSuite {
  private val namespace  = "idkqeinxdlui"
  private val bucketName = "zio-oci-objectstorage"

  private def md5Digest = ZSink
    .foldLeftChunks(MessageDigest.getInstance("MD5")) { (digester, chunk: Chunk[Byte]) =>
      digester.update(chunk.toArray)
      digester
    }
    .map(d => String.format("%032x", new java.math.BigInteger(1, d.digest())))

  def spec(label: String): Spec[ObjectStorage, Exception] =
    suite(label)(
      test("listAllObjects") {
        for {
          list <- listAllObjects(namespace, bucketName).runCollect
        } yield assert(list.map(_.getName()))(Assertion.hasSameElements(List("first", "second", "third")))
      },
      test("listAllObjects with prefix") {
        for {
          list <- listAllObjects(namespace, bucketName, ListObjectsOptions(Some("f"), None, None, 1, Set(ListObjectsOptions.Field.Name))).runCollect
        } yield assert(list.lastOption.map(_.getName()))(Assertion.equalTo(Some("first")))
      },
      test("listAllObjects with prefix and size") {
        for {
          list <- listAllObjects(
            namespace,
            bucketName,
            ListObjectsOptions(Some("s"), None, None, 1, Set(ListObjectsOptions.Field.Name, ListObjectsOptions.Field.Size))
          ).runCollect
        } yield assert(list.lastOption.map(_.getSize().toLong))(Assertion.equalTo(Option(10000000L)))
      },
      test("getObject and compare size") {
        for {
          o <- (getObject(namespace, bucketName, "first") >>> ZPipeline.utf8Decode).runCollect
        } yield assert(o.mkString.length)(Assertion.equalTo(4))
      },
      test("getObject and compare md5 digest") {
        for {
          o <- getObject(namespace, bucketName, "second").run(md5Digest)
        } yield assert(o)(Assertion.equalTo("55c783984393732b474914dbf3881240"))
      },
      test("getObject by range") {
        for {
          o <- getObject(namespace, bucketName, "second", GetObjectOptions(Some(GetObjectOptions.Range(Some(9999000), None)))).runCollect
        } yield assert(o.size)(Assertion.equalTo(1000))
      }
    )
}
