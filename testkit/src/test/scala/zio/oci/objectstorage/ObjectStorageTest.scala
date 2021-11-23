package zio.oci.objectstorage

import zio.{Chunk, Has, ZLayer}
import zio.blocking.Blocking
import zio.nio.core.file.Path
import zio.stream.ZTransducer
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test._

object ObjectStorageTestSpec extends DefaultRunnableSpec {
  private val root = Path("test-data")

  private val testStub: ZLayer[Blocking, Any, Has[ObjectStorage]] = ZLayer.fromFunction(Test.connect(root))

  private val objectStorage: ZLayer[Blocking, TestFailure[Any], Has[ObjectStorage]] =
    testStub.mapError(TestFailure.fail)

  override def spec =
    ObjectStorageSuite.spec.provideCustomLayerShared(Blocking.live >>> objectStorage)
}

object ObjectStorageSuite {
  val compartmentId = "compartmentId"
  val namespace     = "namespace"
  val bucketName    = "bucket"

  def spec: Spec[Has[ObjectStorage] with Blocking, TestFailure[Exception], TestSuccess] =
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
      testM("get object no Range") {
        for {
          content <- getObject(namespace, bucketName, "LaserDisc").transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("LaserDiscs were invented in 1978 (same year I was born) and were so cool"))
      },
      testM("get object Range.Exact") {
        for {
          content <- getObject(namespace, bucketName, "LaserDisc", Some(Range.Exact(2, 5))).transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("ser"))
      },
      testM("get object Range.From") {
        for {
          content <- getObject(namespace, bucketName, "LaserDisc", Some(Range.From(28))).transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("1978 (same year I was born) and were so cool"))
      },
      testM("get object Range.Last") {
        for {
          content <- getObject(namespace, bucketName, "LaserDisc", Some(Range.Last(7))).transduce(ZTransducer.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("so cool"))
      }
    )
}
