package zio.oci.objectstorage

import zio.{Chunk, Layer, ZLayer}

import zio.nio.file.Path
import zio.stream.ZPipeline
import zio.test.Assertion.{equalTo, hasSameElements}
import zio.test._
import zio.test.ZIOSpecDefault

object ObjectStorageTestSpec extends ZIOSpecDefault {
  private val root = Path("test-data")

  private val testStub: Layer[Any, ObjectStorage] = ZLayer.succeed(Test.connect(root))

  private val objectStorage: Layer[TestFailure[Any], ObjectStorage] =
    testStub.mapError(TestFailure.fail)

  override def spec =
    ObjectStorageSuite.spec.provideLayerShared(objectStorage)
}

object ObjectStorageSuite {
  val compartmentId = "compartmentId"
  val namespace     = "namespace"
  val bucketName    = "bucket"

  def spec: Spec[ObjectStorage, Exception] =
    suite("ObjectStorageTestSpec")(
      test("list buckets") {
        for {
          buckets <- listBuckets(compartmentId, namespace)
        } yield assert(buckets.bucketSummaries.map(_.getName))(equalTo(Chunk.single(bucketName)))
      },
      test("list objects") {
        for {
          list <- listObjects(namespace, bucketName)
        } yield assert(list.objectSummaries.map(_.getName()))(hasSameElements(List("Wikipedia/Redis", "LaserDisc")))
      },
      test("list objects after") {
        for {
          list <- listObjects(namespace, bucketName, ListObjectsOptions(None, None, Some("LaserDisc"), 100, Set(ListObjectsOptions.Field.Name)))
        } yield assert(list.objectSummaries.map(_.getName()))(equalTo(Chunk.single("Wikipedia/Redis")))
      },
      test("get object") {
        for {
          content <- (getObject(namespace, bucketName, "LaserDisc") >>> ZPipeline.utf8Decode).runCollect
        } yield assert(content.mkString)(equalTo("LaserDiscs were invented in 1978 (same year I was born) and were so cool"))
      }
    )
}
