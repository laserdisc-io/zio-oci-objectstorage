package zio.oci.objectstorage

import com.oracle.bmc.Region
import com.oracle.bmc.auth.{
  BasicAuthenticationDetailsProvider,
  ConfigFileAuthenticationDetailsProvider,
  InstancePrincipalsAuthenticationDetailsProvider
}
import zio._
import zio.blocking.effectBlocking

final case class ObjectStorageAuth(auth: BasicAuthenticationDetailsProvider)

object ObjectStorageAuth {
  private[this] def load[R](
      provider: RManaged[R, BasicAuthenticationDetailsProvider]
  )(
      f: BasicAuthenticationDetailsProvider => RIO[R, BasicAuthenticationDetailsProvider]
  ): ZIO[R, InvalidAuthDetails, ObjectStorageAuth] =
    provider.use(f).map(ObjectStorageAuth.apply).mapError(e => InvalidAuthDetails(e.getMessage()))

  def const(auth: BasicAuthenticationDetailsProvider): UIO[ObjectStorageAuth] =
    UIO.succeed(ObjectStorageAuth(auth))

  val fromConfigFileDefaultProfile = fromConfigFileProfile("DEFAULT")

  def fromConfigFileProfile(profile: String) =
    load(Managed.fromEffect(effectBlocking(new ConfigFileAuthenticationDetailsProvider(profile))))(p => IO(p))

  val fromInstancePrincipals =
    load(Managed.fromEffect(effectBlocking(InstancePrincipalsAuthenticationDetailsProvider.builder().build())))(p => IO(p))
}

final case class ObjectStorageSettings(region: Region, auth: ObjectStorageAuth)
