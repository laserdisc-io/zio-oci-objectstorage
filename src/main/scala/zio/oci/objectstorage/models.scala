package zio.oci.objectstorage

import com.oracle.bmc.objectstorage.model.{BucketSummary, ObjectSummary}
import com.oracle.bmc.objectstorage.responses.{ListBucketsResponse, ListObjectsResponse}
import zio.Chunk

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
