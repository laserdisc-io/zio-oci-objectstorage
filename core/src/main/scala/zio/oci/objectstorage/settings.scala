package zio.oci.objectstorage

import com.oracle.bmc.Region
import com.oracle.bmc.auth.{
  BasicAuthenticationDetailsProvider,
  ConfigFileAuthenticationDetailsProvider,
  InstancePrincipalsAuthenticationDetailsProvider
}
import zio.{Scope, UIO, ZIO}

final case class ObjectStorageAuth(auth: BasicAuthenticationDetailsProvider)

object ObjectStorageAuth {
  private[this] def load[R](
      provider: ZIO[R with Scope, Throwable, BasicAuthenticationDetailsProvider]
  )(
      f: BasicAuthenticationDetailsProvider => ZIO[R, Throwable, BasicAuthenticationDetailsProvider]
  ): ZIO[R, InvalidAuthDetails, ObjectStorageAuth] =
    ZIO
      .scoped[R] {
        provider.flatMap(f)
      }
      .map(ObjectStorageAuth.apply)
      .mapError(e => InvalidAuthDetails(e.getMessage()))

  def const(auth: BasicAuthenticationDetailsProvider): UIO[ObjectStorageAuth] =
    ZIO.succeed(ObjectStorageAuth(auth))

  val fromConfigFileDefaultProfile = fromConfigFileProfile("DEFAULT")

  def fromConfigFileProfile(profile: String) =
    load(ZIO.attemptBlockingIO(new ConfigFileAuthenticationDetailsProvider(profile)))(p => ZIO.attempt(p))

  val fromInstancePrincipals =
    load(ZIO.attemptBlockingIO(InstancePrincipalsAuthenticationDetailsProvider.builder().build()))(p => ZIO.attempt(p))
}

final case class ObjectStorageSettings(region: Region, auth: ObjectStorageAuth)
