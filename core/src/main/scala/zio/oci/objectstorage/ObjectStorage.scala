package zio.oci.objectstorage

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.ObjectSummary
import zio.{IO, Layer, ZIO, ZLayer}
import zio.stream.{Stream, ZStream}

trait ObjectStorage { self =>
  def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ObjectStorageBucketListing]

  def listObjects(namespace: String, bucketName: String): IO[BmcException, ObjectStorageObjectListing] =
    listObjects(namespace, bucketName, ListObjectsOptions.default)

  def listObjects(namespace: String, bucketName: String, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

  def listAllObjects(namespace: String, bucketName: String): Stream[BmcException, ObjectSummary] =
    listAllObjects(namespace, bucketName, ListObjectsOptions.default)

  def listAllObjects(namespace: String, bucketName: String, options: ListObjectsOptions): Stream[BmcException, ObjectSummary] =
    ZStream
      .fromZIO(self.listObjects(namespace, bucketName, options))
      .flatMap(listing => paginateObjects(listing, options).mapConcat(_.objectSummaries))

  def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): Stream[BmcException, ObjectStorageObjectListing] =
    ZStream.paginateZIO(initialListing) {
      case current @ ObjectStorageObjectListing(_, _, _, None) => ZIO.succeed(current -> None)
      case current                                             => self.getNextObjects(current, options).map(next => current -> Some(next))
    }

  def getObject(namespace: String, bucketName: String, name: String): Stream[BmcException, Byte] =
    getObject(namespace, bucketName, name, GetObjectOptions.default)

  def getObject(namespace: String, bucketName: String, name: String, options: GetObjectOptions): Stream[BmcException, Byte]
}

object ObjectStorage {
  def live(settings: ObjectStorageSettings): Layer[ConnectionError, ObjectStorage] =
    ZLayer.scoped(Live.connect(settings))
}
