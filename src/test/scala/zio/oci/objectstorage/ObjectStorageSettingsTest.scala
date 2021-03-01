package zio.oci.objectstorage

import zio.test.DefaultRunnableSpec
import zio.test.{assert, assertM}
import zio.test.Assertion.{equalTo, isNonEmptyString}
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import java.util.UUID

object ObjectStorageSettingsTest extends DefaultRunnableSpec {
  def spec =
    suite("Settings")(
      suite("auth")(
        testM("const auth") {
          assertM(ObjectStorageAuth.const(SimpleAuthenticationDetailsProvider.builder().build()).map(_.auth.getKeyId()))(
            equalTo(ObjectStorageAuth(SimpleAuthenticationDetailsProvider.builder().build()).auth.getKeyId())
          )
        },
        testM("no config file or invalid profile") {
          for {
            f <- ObjectStorageAuth.fromConfigFileProfile(UUID.randomUUID().toString()).flip.map(_.message)
          } yield assert(f)(isNonEmptyString)
        }
      )
    )
}
