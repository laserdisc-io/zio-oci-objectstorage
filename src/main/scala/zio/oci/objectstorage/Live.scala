package zio.oci.objectstorage

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
  override def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ListBucketsResponse] =
    listBuckets(ListBucketsRequest.builder().compartmentId(compartmentId).namespaceName(namespace).build())

  private def listBuckets(request: ListBucketsRequest): IO[BmcException, ListBucketsResponse] =
    execute[ListBucketsRequest, ListBucketsResponse] { c =>
      c.listBuckets(request, _: AsyncHandler[ListBucketsRequest, ListBucketsResponse])
    }

  override def listObjects(namespace: String, bucket: String): IO[BmcException, ListObjectsResponse] =
    execute[ListObjectsRequest, ListObjectsResponse] { c =>
      c.listObjects(
        ListObjectsRequest.builder().bucketName(bucket).namespaceName(namespace).build(),
        _: AsyncHandler[ListObjectsRequest, ListObjectsResponse]
      )
    }

  override def getObject(namespace: String, bucket: String, name: String): ZStream[Blocking, BmcException, Byte] =
    ZStream
      .fromEffect(execute[GetObjectRequest, GetObjectResponse] { c =>
        c.getObject(
          GetObjectRequest.builder().namespaceName(namespace).bucketName(bucket).objectName(name).build(),
          _: AsyncHandler[GetObjectRequest, GetObjectResponse]
        )
      })
      .flatMap(is => ZStream.fromInputStreamEffect(IO(is.getInputStream()).refineToOrDie[IOException]))
      .refineToOrDie[BmcException]

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
          ObjectStorageAsyncClient.builder().region(settings.region).build(settings.auth.auth)
        }
      )
      .map(new Live(_))
      .mapError(e => ConnectionError(e.getMessage(), e.getCause()))
}
