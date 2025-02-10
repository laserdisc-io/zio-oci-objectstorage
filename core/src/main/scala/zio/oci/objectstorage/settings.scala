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
    load(ZIO.attemptBlocking(new ConfigFileAuthenticationDetailsProvider(profile)))(p => ZIO.attempt(p))

  val fromInstancePrincipals =
    load(ZIO.attemptBlocking(InstancePrincipalsAuthenticationDetailsProvider.builder().build()))(p => ZIO.attempt(p))
}

final case class ObjectStorageSettings(region: Region, auth: ObjectStorageAuth)
