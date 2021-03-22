package zio.oci.objectstorage

import zio.ZLayer
import zio.blocking.Blocking
import zio.nio.core.file.Path
import zio.test._
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.Chunk
import zio.stream.ZTransducer

object ObjectStorageTestSpec extends DefaultRunnableSpec {
  private val root = Path("test-data")

  private val objectStorage: ZLayer[Blocking, TestFailure[Any], ObjectStorage] =
    zio.oci.objectstorage.stub(root).mapError(TestFailure.fail)

  override def spec =
    ObjectStorageSuite.spec.provideCustomLayerShared(Blocking.live >>> objectStorage)
}

object ObjectStorageSuite {
  val compartmentId = "compartmentId"
  val namespace     = "namespace"
  val bucketName    = "bucket"

  def spec: Spec[ObjectStorage with Blocking, TestFailure[Exception], TestSuccess] =
    suite("ObjectStorageTestSpec")(
      testM("list buckets") {
        for {
          buckets <- listBuckets(compartmentId, namespace)
        } yield assert(buckets.bucketSummaries.map(_.getName))(equalTo(Chunk.single(bucketName)))
      },
      testM("list objects") {
        for {
          list <- listObjects(namespace, bucketName)
        } yield assert(list.objectSummaries.map(_.getName()))(hasSameElements(List("Wikipedia/Redis", "LaserDisc")))
      },
      testM("list objects after") {
        for {
          list <- listObjects(namespace, bucketName, ListObjectsOptions(None, None, Some("LaserDisc"), 100))
        } yield assert(list.objectSummaries.map(_.getName()))(equalTo(Chunk.single("Wikipedia/Redis")))
      },
      testM("get object") {
        for {
          content <- getObject(namespace, bucketName, "LaserDisc").transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("LaserDiscs were invented in 1978 (same year I was born) and were so cool"))
      }
    )
}
