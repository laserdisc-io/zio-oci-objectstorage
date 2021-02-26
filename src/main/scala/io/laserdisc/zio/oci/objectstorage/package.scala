package io.laserdisc.zio.oci

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.responses.{GetObjectResponse, ListBucketsResponse, ListObjectsResponse}
import zio._
import zio.stream.{Stream, ZStream}
import zio.blocking.Blocking

package object objectstorage {
  type ObjectStorage = Has[ObjectStorage.Service]

  object ObjectStorage {
    trait Service { self =>
      def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ListBucketsResponse]
      def listObjects(namespace: String, bucket: String): IO[BmcException, ListObjectsResponse]
      def getObject(namespace: String, bucket: String, name: String): ZStream[Blocking, BmcException, Byte]
    }
  }

  def live(settings: ObjectStorageSettings): Layer[ConnectionError, ObjectStorage] =
    ZLayer.fromManaged(Live.connect(settings))

  def listBuckets(compartmentId: String, namespace: String): ZIO[ObjectStorage, BmcException, ListBucketsResponse] =
    ZIO.accessM(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[ObjectStorage, BmcException, ListObjectsResponse] =
    ZIO.accessM(_.get.listObjects(namespace, bucket))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage with Blocking, BmcException, Byte] =
    ZStream.accessStream(_.get.getObject(namespace, bucket, name))
}
