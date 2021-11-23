package zio.oci.objectstorage

final case class ListObjectsOptions(prefix: Option[String], start: Option[String], startAfter: Option[String], limit: Int)

object ListObjectsOptions {
  val default: ListObjectsOptions = ListObjectsOptions(None, None, None, Limit.Max)

  def oneAfter(name: String): ListObjectsOptions = ListObjectsOptions(None, None, Some(name), 1)
}

object Limit {
  val Max: Int = 1000
}

sealed trait Range {
  def asOCI: com.oracle.bmc.model.Range
}
object Range {
  final case class Exact(fromByte: Long, toByte: Long) extends Range { override final val asOCI = new com.oracle.bmc.model.Range(fromByte, toByte) }
  final case class From(byte: Long)                    extends Range { override final val asOCI = new com.oracle.bmc.model.Range(byte, null)       }
  final case class Last(bytes: Int)                    extends Range { override final val asOCI = new com.oracle.bmc.model.Range(null, bytes)      }
}
