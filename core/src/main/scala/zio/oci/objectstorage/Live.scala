package zio.oci.objectstorage

import com.oracle.bmc.http.client.Options
import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient
import com.oracle.bmc.objectstorage.requests.{GetObjectRequest, ListBucketsRequest, ListObjectsRequest}
import com.oracle.bmc.objectstorage.responses.{GetObjectResponse, ListBucketsResponse, ListObjectsResponse}
import com.oracle.bmc.responses.AsyncHandler
import zio.{Chunk, IO, ZIO}
import zio.stream.{Stream, ZStream}

import java.io.IOException
import java.util.concurrent.{Future => JFuture}

final class Live(unsafeClient: ObjectStorageAsyncClient) extends ObjectStorage {
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

  private def getObject(request: GetObjectRequest): Stream[BmcException, Byte] =
    ZStream
      .fromZIO(execute[GetObjectRequest, GetObjectResponse] { c =>
        c.getObject(request, _: AsyncHandler[GetObjectRequest, GetObjectResponse])
      })
      .flatMap(is => ZStream.fromInputStreamZIO(ZIO.attemptBlockingIO(is.getInputStream()).refineToOrDie[IOException]))
      .refineToOrDie[BmcException]

  override def getObject(namespace: String, bucket: String, name: String, options: GetObjectOptions): Stream[BmcException, Byte] =
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
    ZIO.async[Any, BmcException, O] { cb =>
      f(unsafeClient)(
        new AsyncHandler[I, O] {
          override def onSuccess(request: I, response: O): Unit    = cb(ZIO.succeed(response))
          override def onError(request: I, error: Throwable): Unit = cb(ZIO.fail(error).refineToOrDie[BmcException])
        }
      )
      ()
    }
}

object Live {
  def connect(settings: ObjectStorageSettings) =
    ZIO
      .fromAutoCloseable(
        ZIO.attempt {
          // disable sdk's stream auto-close as it's handled by ZStream.fromInputStreamEffect
          // https://github.com/oracle/oci-java-sdk/blob/v3.0.0/ApacheConnector-README.md#switching-off-auto-close-of-streams
          Options.shouldAutoCloseResponseInputStream(false)
          ObjectStorageAsyncClient.builder().region(settings.region).build(settings.auth.auth)
        }
      )
      .map(new Live(_))
      .mapError(e => ConnectionError(e.getMessage(), e.getCause()))
}
