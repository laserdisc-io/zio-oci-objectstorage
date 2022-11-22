package zio.oci.objectstorage

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import zio.test.{ZIOSpecDefault, assert, assertZIO}
import zio.test.Assertion.{equalTo, isNonEmptyString}

import java.util.UUID

object ObjectStorageSettingsTest extends ZIOSpecDefault {
  def spec =
    suite("Settings")(
      suite("auth")(
        test("const auth") {
          assertZIO(ObjectStorageAuth.const(SimpleAuthenticationDetailsProvider.builder().build()).map(_.auth.getKeyId()))(
            equalTo(ObjectStorageAuth(SimpleAuthenticationDetailsProvider.builder().build()).auth.getKeyId())
          )
        },
        test("no config file or invalid profile") {
          for {
            f <- ObjectStorageAuth.fromConfigFileProfile(UUID.randomUUID().toString()).flip.map(_.message)
          } yield assert(f)(isNonEmptyString)
        }
      )
    )
}
