package zio.oci.objectstorage

import com.oracle.bmc.Options
import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.responses.{GetObjectResponse, ListBucketsResponse, ListObjectsResponse}
import com.oracle.bmc.objectstorage.requests.{GetObjectRequest, ListBucketsRequest, ListObjectsRequest}
import com.oracle.bmc.responses.AsyncHandler
import zio._
import zio.blocking.Blocking
import zio.stream.ZStream

import java.util.concurrent.{Future => JFuture}
import java.io.IOException

final class Live(unsafeClient: ObjectStorageAsyncClient) extends ObjectStorage.Service {
  override def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ObjectStorageBucketListing] =
    listBuckets(
      ListBucketsRequest
        .builder()
        .compartmentId(compartmentId)
        .namespaceName(namespace)
        .build()
    ).map(r => ObjectStorageBucketListing.from(compartmentId, namespace, r))

  private def listBuckets(request: ListBucketsRequest): IO[BmcException, ListBucketsResponse] =
    execute[ListBucketsRequest, ListBucketsResponse] { c =>
      c.listBuckets(request, _: AsyncHandler[ListBucketsRequest, ListBucketsResponse])
    }

  override def listObjects(namespace: String, bucketName: String, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing] =
    listObjects(
      ListObjectsRequest
        .builder()
        .bucketName(bucketName)
        .namespaceName(namespace)
        .prefix(options.prefix.orNull)
        .start(options.start.orNull)
        .startAfter(options.startAfter.orNull)
        .limit(options.limit)
        .fields(options.fields.map(_.value).mkString(","))
        .build()
    ).map(r => ObjectStorageObjectListing.from(namespace, bucketName, r))

  private def listObjects(request: ListObjectsRequest): IO[BmcException, ListObjectsResponse] =
    execute[ListObjectsRequest, ListObjectsResponse] { c =>
      c.listObjects(request, _: AsyncHandler[ListObjectsRequest, ListObjectsResponse])
    }

  override def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing] =
    listing.nextStartWith.fold[ZIO[Any, BmcException, ObjectStorageObjectListing]](
      ZIO.succeed(listing.copy(objectSummaries = Chunk.empty))
    ) { start =>
      listObjects(
        ListObjectsRequest
          .builder()
          .namespaceName(listing.namespace)
          .bucketName(listing.bucketName)
          .start(start)
          .prefix(options.prefix.orNull)
          .limit(options.limit)
          .fields(options.fields.map(_.value).mkString(","))
          .build()
      ).map(r => ObjectStorageObjectListing.from(listing.namespace, listing.bucketName, r))
    }

  private def getObject(request: GetObjectRequest): ZStream[Blocking, BmcException, Byte] =
    ZStream
      .fromEffect(execute[GetObjectRequest, GetObjectResponse] { c =>
        c.getObject(request, _: AsyncHandler[GetObjectRequest, GetObjectResponse])
      })
      .flatMap(is => ZStream.fromInputStreamEffect(IO(is.getInputStream()).refineToOrDie[IOException]))
      .refineToOrDie[BmcException]

  override def getObject(namespace: String, bucket: String, name: String, options: GetObjectOptions): ZStream[Blocking, BmcException, Byte] =
    getObject(
      GetObjectRequest
        .builder()
        .namespaceName(namespace)
        .bucketName(bucket)
        .objectName(name)
        .range(options.range.map(r => new com.oracle.bmc.model.Range(r.startByte.map(Long.box).orNull, r.endByte.map(Long.box).orNull)).orNull)
        .build()
    )

  def execute[I, O](f: ObjectStorageAsyncClient => Function1[AsyncHandler[I, O], JFuture[O]]): IO[BmcException, O] =
    IO.effectAsync[BmcException, O] { cb =>
      f(unsafeClient)(
        new AsyncHandler[I, O] {
          override def onSuccess(request: I, response: O): Unit    = cb(IO.succeed(response))
          override def onError(request: I, error: Throwable): Unit = cb(IO.fail(error).refineToOrDie[BmcException])
        }
      )
    }
}

object Live {
  def connect(settings: ObjectStorageSettings) =
    ZManaged
      .fromAutoCloseable(
        Task {
          // disable sdk's stream auto-close as it's handled by ZStream.fromInputStreamEffect
          // https://github.com/oracle/oci-java-sdk/blob/v2.46.0/ApacheConnector-README.md#switching-off-auto-close-of-streams
          Options.shouldAutoCloseResponseInputStream(false)
          ObjectStorageAsyncClient.builder().region(settings.region).build(settings.auth.auth)
        }
      )
      .map(new Live(_))
      .mapError(e => ConnectionError(e.getMessage(), e.getCause()))
}
