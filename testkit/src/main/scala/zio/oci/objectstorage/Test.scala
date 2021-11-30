package zio.oci.objectstorage

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.{BucketSummary, ObjectSummary}
import zio.{Chunk, IO, Task, ZIO, ZManaged}
import zio.blocking.Blocking
import zio.nio.core.file.Path
import zio.nio.file.Files
import zio.stream.ZStream

import java.io.{File, FileInputStream, FileNotFoundException, IOException}
import java.nio.file.attribute.PosixFileAttributes

object Test {
  private def fileNotFound(err: String): BmcException =
    new BmcException(404, "ObjectNotFound", "Object not found", "00000000-0000-0000-0000-000000000000", new FileNotFoundException(err))
  private def streamFrom(f: File, maybeRange: Option[Range]): ZStream[Blocking, IOException, Byte] = {
    val stream = ZStream.fromInputStreamManaged(ZManaged.fromAutoCloseable(Task(new FileInputStream(f)).refineToOrDie[IOException]), 2048)
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

      override def getObject(
          namespace: String,
          bucketName: String,
          name: String,
          maybeRange: Option[Range]
      ): IO[BmcException, ObjectStorageObjectContent] =
        IO.succeed((path / namespace / bucketName / name).toFile).flatMap {
          case f if !(f.exists && f.canRead) => IO.fail(fileNotFound(s"cannot find file ${f.getAbsolutePath()}"))
          case f                             => IO.succeed(ObjectStorageObjectContent(f.length(), streamFrom(f, maybeRange)))
        }
    }
  }
}
