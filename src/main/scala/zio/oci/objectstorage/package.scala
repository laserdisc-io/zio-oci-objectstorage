package zio.oci

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse
import zio._
import zio.stream.{Stream, ZStream}
import zio.blocking.Blocking

package object objectstorage {
  type ObjectStorage          = Has[ObjectStorage.Service]
  type ObjectStorageStream[A] = ZStream[ObjectStorage, BmcException, A]

  object ObjectStorage {
    trait Service { self =>
      def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ListBucketsResponse]

      def listObjects(namespace: String, bucketName: String): IO[BmcException, ObjectStorageObjectListing]

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

  def listBuckets(compartmentId: String, namespace: String): ZIO[ObjectStorage, BmcException, ListBucketsResponse] =
    ZIO.accessM(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.listObjects(namespace, bucket))

  def getNextObjects(listing: ObjectStorageObjectListing): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.accessM(_.get.getNextObjects(listing))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name))

  def paginateObjects(initialListing: ObjectStorageObjectListing): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.accessStream[ObjectStorage](_.get.paginateObjects(initialListing))
}
