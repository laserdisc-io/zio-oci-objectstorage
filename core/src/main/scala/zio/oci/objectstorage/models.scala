package zio.oci.objectstorage

import com.oracle.bmc.objectstorage.model.{BucketSummary, ObjectSummary}
import com.oracle.bmc.objectstorage.responses.{GetObjectResponse, ListBucketsResponse, ListObjectsResponse}
import zio.{Chunk, IO}
import zio.blocking.Blocking
import zio.stream.{Stream, ZStream}

import java.io.IOException
import scala.jdk.CollectionConverters._

final case class ObjectStorageObjectListing(
    namespace: String,
    bucketName: String,
    objectSummaries: Chunk[ObjectSummary],
    nextStartWith: Option[String]
)

object ObjectStorageObjectListing {
  def from(namespace: String, bucketName: String, r: ListObjectsResponse): ObjectStorageObjectListing =
    ObjectStorageObjectListing(
      namespace,
      bucketName,
      Chunk.fromIterable(r.getListObjects().getObjects().asScala),
      Option(r.getListObjects().getNextStartWith())
    )
}

final case class ObjectStorageBucketListing(compartmentId: String, namespace: String, bucketSummaries: Chunk[BucketSummary], nextPage: Option[String])

object ObjectStorageBucketListing {
  def from(compartmentId: String, namespace: String, r: ListBucketsResponse): ObjectStorageBucketListing =
    ObjectStorageBucketListing(
      compartmentId,
      namespace,
      Chunk.fromIterable(r.getItems().asScala),
      Option(r.getOpcNextPage())
    )
}

final case class ObjectStorageObjectContent(contentLength: Long, byteStream: ZStream[Blocking, IOException, Byte])

object ObjectStorageObjectContent {
  def from(r: GetObjectResponse): ObjectStorageObjectContent =
    ObjectStorageObjectContent(
      r.getContentLength(),
      Stream.fromInputStreamEffect(IO(r.getInputStream()).refineToOrDie[IOException])
    )
}
