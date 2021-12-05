package zio.oci.objectstorage

final case class ListObjectsOptions(
    prefix: Option[String],
    start: Option[String],
    startAfter: Option[String],
    limit: Int,
    fields: Set[ListObjectsOptions.Field]
)

object ListObjectsOptions {
  sealed abstract class Field(val value: String)
  object Field {
    case object Name          extends Field("name")
    case object Size          extends Field("size")
    case object Etag          extends Field("etag")
    case object TimeCreated   extends Field("timeCreated")
    case object Md5           extends Field("md5")
    case object TimeModified  extends Field("timeModified")
    case object StorageTier   extends Field("storageTier")
    case object ArchivalState extends Field("archivalState")
  }
  val default: ListObjectsOptions = ListObjectsOptions(None, None, None, Limit.Max, Set(Field.Name, Field.Size))

  def oneAfter(name: String): ListObjectsOptions = ListObjectsOptions(None, None, Some(name), 1, Set(Field.Name, Field.Size))
}

object Limit {
  val Max: Int = 1000
}
