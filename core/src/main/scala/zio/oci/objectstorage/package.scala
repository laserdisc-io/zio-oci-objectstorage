package zio.oci

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.ObjectSummary
import zio._
import zio.blocking.Blocking
import zio.stream.{Stream, ZStream}

package object objectstorage {
  type ObjectStorage          = Has[ObjectStorage.Service]
  type ObjectStorageStream[A] = ZStream[ObjectStorage, BmcException, A]

  object ObjectStorage {
    trait Service { self =>
      def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ObjectStorageBucketListing]

      def listObjects(namespace: String, bucketName: String): IO[BmcException, ObjectStorageObjectListing] =
        listObjects(namespace, bucketName, ListObjectsOptions.default)

      def listObjects(namespace: String, bucketName: String, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

      def listAllObjects(namespace: String, bucketName: String): Stream[BmcException, ObjectSummary] =
        listAllObjects(namespace, bucketName, ListObjectsOptions.default)

      def listAllObjects(namespace: String, bucketName: String, options: ListObjectsOptions): Stream[BmcException, ObjectSummary] =
        ZStream
          .fromEffect(self.listObjects(namespace, bucketName, options))
          .flatMap(listing => paginateObjects(listing, options).mapConcat(_.objectSummaries))

      def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

      def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): Stream[BmcException, ObjectStorageObjectListing] =
        ZStream.paginateM(initialListing) {
          case current @ ObjectStorageObjectListing(_, _, _, None) => ZIO.succeed(current -> None)
          case current                                             => self.getNextObjects(current, options).map(next => current -> Some(next))
        }

      def getObject(namespace: String, bucketName: String, name: String): ZStream[Blocking, BmcException, Byte] =
        getObject(namespace, bucketName, name, GetObjectOptions.default)

      def getObject(namespace: String, bucketName: String, name: String, options: GetObjectOptions): ZStream[Blocking, BmcException, Byte]
    }
  }

  def live(settings: ObjectStorageSettings): Layer[ConnectionError, ObjectStorage] =
    ZLayer.fromManaged(Live.connect(settings))

  def listBuckets(compartmentId: String, namespace: String): ZIO[ObjectStorage, BmcException, ObjectStorageBucketListing] =
    ZIO.accessM(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.listObjects(namespace, bucket))

  def listObjects(namespace: String, bucket: String, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.listObjects(namespace, bucket, options))

  def listAllObjects(namespace: String, bucket: String) = ZStream.accessStream[ObjectStorage](_.get.listAllObjects(namespace, bucket))

  def listAllObjects(namespace: String, bucket: String, options: ListObjectsOptions) =
    ZStream.accessStream[ObjectStorage](_.get.listAllObjects(namespace, bucket, options))

  def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.getNextObjects(listing, options))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name))

  def getObject(
      namespace: String,
      bucket: String,
      name: String,
      options: GetObjectOptions
  ): ZStream[ObjectStorage with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name, options))

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.accessStream[ObjectStorage](_.get.paginateObjects(initialListing, options))
}
