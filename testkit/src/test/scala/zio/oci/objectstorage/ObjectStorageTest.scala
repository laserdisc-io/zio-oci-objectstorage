/*
 * Copyright (c) 2021-2025 LaserDisc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
