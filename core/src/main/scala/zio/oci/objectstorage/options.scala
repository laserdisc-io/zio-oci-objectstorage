package zio.oci.objectstorage

final case class ListObjectsOptions(prefix: Option[String], start: Option[String], startAfter: Option[String], limit: Int)

object ListObjectsOptions {
  val default: ListObjectsOptions = ListObjectsOptions(None, None, None, Limit.Max)

  def oneAfter(name: String): ListObjectsOptions = ListObjectsOptions(None, None, Some(name), 1)
}

object Limit {
  val Max: Int = 1000
}
