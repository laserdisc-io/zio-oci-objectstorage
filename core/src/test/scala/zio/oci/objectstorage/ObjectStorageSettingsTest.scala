package zio.oci.objectstorage

import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider
import zio.test.{ZIOSpecDefault, assertZIO}
import zio.test.Assertion.{equalTo, isLeft}

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
          assertZIO(ObjectStorageAuth.fromConfigFileProfile(UUID.randomUUID().toString()).either)(isLeft)
        }
      )
    )
}
