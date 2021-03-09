package zio.oci.objectstorage

final case class ListObjectsOptions(prefix: Option[String])

object ListObjectsOptions {
  val default: ListObjectsOptions = ListObjectsOptions(None)
}
