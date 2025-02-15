/*
 * Copyright (c) 2021-2025 LaserDisc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

final case class GetObjectOptions(range: Option[GetObjectOptions.Range])

object GetObjectOptions {
  final case class Range(startByte: Option[Long], endByte: Option[Long])

  val default: GetObjectOptions = GetObjectOptions(None)
}

object Limit {
  val Max: Int = 1000
}
