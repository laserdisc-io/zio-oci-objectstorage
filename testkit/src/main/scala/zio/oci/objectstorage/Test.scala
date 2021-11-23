package zio.oci.objectstorage

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.{BucketSummary, ObjectSummary}
import zio.{Chunk, IO, Task, ZIO, ZManaged}
import zio.blocking.Blocking
import zio.nio.core.file.Path
import zio.nio.file.Files
import zio.stream.ZStream

import java.io.{FileInputStream, FileNotFoundException, IOException, InputStream}
import java.nio.file.attribute.PosixFileAttributes

object Test {
  private def fileNotFound(err: FileNotFoundException): BmcException =
    new BmcException(404, "ObjectNotFound", "Object not found", "00000000-0000-0000-0000-000000000000", err)
  private def streamFrom(is: InputStream, maybeRange: Option[Range]): ZStream[Blocking, IOException, Byte] = {
    val stream = ZStream.fromInputStream(is, 2048)
    maybeRange.fold(stream) {
      case Range.Exact(from, to) => stream.drop(from).take(to - from)
      case Range.From(byte)      => stream.drop(byte)
      case Range.Last(bytes)     => stream.takeRight(bytes)
    }
  }

  def connect(path: Path): Blocking => ObjectStorage = { blocking =>
    new ObjectStorage {
      override def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ObjectStorageBucketListing] =
        Files
          .list(path / namespace)
          .filterM(p => Files.readAttributes[PosixFileAttributes](p).map(_.isDirectory()))
          .map { p =>
            BucketSummary.builder().name(p.filename.toString()).build()
          }
          .runCollect
          .map(c => ObjectStorageBucketListing(compartmentId, namespace, c, None))
          .orDie
          .provide(blocking)

      override def listObjects(namespace: String, bucketName: String, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing] =
        Files
          .find(path / namespace / bucketName) { case (p, _) =>
            options.prefix.fold(true)(pfx => p.startsWith(path / namespace / bucketName / pfx))
          }
          .mapM(p => Files.readAttributes[PosixFileAttributes](p).map(a => a -> p))
          .filter { case (attr, _) => attr.isRegularFile }
          .map { case (attr, f) =>
            ObjectSummary.builder().name((path / namespace / bucketName).relativize(f).toString()).size(attr.size()).build()
          }
          .runCollect
          .map(
            _.sortBy(_.getName())
              .mapAccum(options.startAfter) {
                case (Some(startWith), o) =>
                  if (startWith.startsWith(o.getName()))
                    None -> Chunk.empty
                  else
                    Some(startWith) -> Chunk.empty
                case (_, o) =>
                  None -> Chunk(o)
              }
              ._2
              .flatten
          )
          .map {
            case list if list.size > options.limit =>
              ObjectStorageObjectListing(namespace, bucketName, list.take(options.limit), None)
            case list =>
              ObjectStorageObjectListing(namespace, bucketName, list, None)
          }
          .orDie
          .provide(blocking)

      override def getNextObjects(listing: ObjectStorageObjectListing, objects: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing] =
        listing.nextStartWith match {
          case Some(startWith) if startWith.nonEmpty => listObjects(listing.namespace, listing.bucketName)
          case _                                     => ZIO.dieMessage("Empty startWith is invalid")
        }

      override def getObject(namespace: String, bucketName: String, name: String, maybeRange: Option[Range]): ZStream[Blocking, BmcException, Byte] =
        ZStream
          .managed(ZManaged.fromAutoCloseable(Task(new FileInputStream((path / namespace / bucketName / name).toFile))))
          .flatMap(streamFrom(_, maybeRange))
          .refineOrDie { case e: FileNotFoundException =>
            fileNotFound(e)
          }
          .provide(blocking)
    }
  }
}
