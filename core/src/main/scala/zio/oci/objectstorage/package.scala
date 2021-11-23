package zio.oci

import com.oracle.bmc.model.BmcException
import zio._
import zio.blocking.Blocking
import zio.stream.ZStream

package object objectstorage {
  type ObjectStorageStream[A] = ZStream[Has[ObjectStorage], BmcException, A]

  def live(settings: ObjectStorageSettings): Layer[ConnectionError, Has[ObjectStorage]] =
    ZLayer.fromManaged(Live.connect(settings))

  def listBuckets(compartmentId: String, namespace: String): ZIO[Has[ObjectStorage], BmcException, ObjectStorageBucketListing] =
    ZIO.accessM(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[Has[ObjectStorage], BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.listObjects(namespace, bucket))

  def listObjects(namespace: String, bucket: String, options: ListObjectsOptions): ZIO[Has[ObjectStorage], BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.listObjects(namespace, bucket, options))

  def listAllObjects(namespace: String, bucket: String) = ZStream.accessStream[Has[ObjectStorage]](_.get.listAllObjects(namespace, bucket))

  def listAllObjects(namespace: String, bucket: String, options: ListObjectsOptions) =
    ZStream.accessStream[Has[ObjectStorage]](_.get.listAllObjects(namespace, bucket, options))

  def getNextObjects(
      listing: ObjectStorageObjectListing,
      options: ListObjectsOptions
  ): ZIO[Has[ObjectStorage], BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.getNextObjects(listing, options))

  def getObject(
      namespace: String,
      bucket: String,
      name: String
  ): ZStream[Has[ObjectStorage] with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name))

  def getObject(
      namespace: String,
      bucket: String,
      name: String,
      maybeRange: Option[Range]
  ): ZStream[Has[ObjectStorage] with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name, maybeRange))

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.accessStream[Has[ObjectStorage]](_.get.paginateObjects(initialListing, options))
}
