package zio.oci.objectstorage

import com.oracle.bmc.objectstorage.model.ObjectSummary
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse
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
