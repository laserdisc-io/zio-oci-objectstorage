package zio.oci

import com.oracle.bmc.model.BmcException
import zio.ZIO
import zio.stream.ZStream

package object objectstorage {
  type ObjectStorageStream[A] = ZStream[ObjectStorage, BmcException, A]

  def listBuckets(compartmentId: String, namespace: String): ZIO[ObjectStorage, BmcException, ObjectStorageBucketListing] =
    ZIO.environmentWithZIO(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.listObjects(namespace, bucket))

  def listObjects(namespace: String, bucket: String, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.listObjects(namespace, bucket, options))

  def listAllObjects(namespace: String, bucket: String) = ZStream.environmentWithStream[ObjectStorage](_.get.listAllObjects(namespace, bucket))

  def listAllObjects(namespace: String, bucket: String, options: ListObjectsOptions) =
    ZStream.environmentWithStream[ObjectStorage](_.get.listAllObjects(namespace, bucket, options))

  def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.getNextObjects(listing, options))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage, BmcException, Byte] =
    ZStream.environmentWithStream(_.get.getObject(namespace, bucket, name))

  def getObject(
      namespace: String,
      bucket: String,
      name: String,
      options: GetObjectOptions
  ): ZStream[ObjectStorage, BmcException, Byte] =
    ZStream.environmentWithStream(_.get.getObject(namespace, bucket, name, options))

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.environmentWithStream[ObjectStorage](_.get.paginateObjects(initialListing, options))
}
