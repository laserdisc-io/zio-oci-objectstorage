package zio.oci.objectstorage

final case class ListObjectsOptions(prefix: Option[String], start: Option[String], startAfter: Option[String], limit: Int)

object ListObjectsOptions {
  val default: ListObjectsOptions = ListObjectsOptions(None, None, None, Limit.Max)
}

object Limit {
  val Max: Int = 1000
}
