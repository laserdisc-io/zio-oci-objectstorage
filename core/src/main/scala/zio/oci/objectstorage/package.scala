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
          .flatMap(paginateObjects(_).mapConcat(_.objectSummaries))

      def getNextObjects(listing: ObjectStorageObjectListing): IO[BmcException, ObjectStorageObjectListing]

      def paginateObjects(initialListing: ObjectStorageObjectListing): Stream[BmcException, ObjectStorageObjectListing] =
        ZStream.paginateM(initialListing) {
          case current @ ObjectStorageObjectListing(_, _, _, None) => ZIO.succeed(current -> None)
          case current                                             => self.getNextObjects(current).map(next => current -> Some(next))
        }

      def getObject(namespace: String, bucketName: String, name: String): ZStream[Blocking, BmcException, Byte]
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

  def getNextObjects(listing: ObjectStorageObjectListing): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.getNextObjects(listing))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name))

  def paginateObjects(initialListing: ObjectStorageObjectListing): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.accessStream[ObjectStorage](_.get.paginateObjects(initialListing))
}
