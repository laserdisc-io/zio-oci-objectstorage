package io.laserdisc.zio.oci.objectstorage

final case class InvalidAuthDetails(message: String)                extends IllegalArgumentException(message)
final case class ConnectionError(message: String, cause: Throwable) extends Exception(message)
