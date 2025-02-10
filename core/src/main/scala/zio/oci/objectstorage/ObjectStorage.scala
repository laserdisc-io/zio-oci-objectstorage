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

import com.oracle.bmc.model.BmcException
import com.oracle.bmc.objectstorage.model.ObjectSummary
import zio.{IO, Layer, ZIO, ZLayer}
import zio.stream.{Stream, ZStream}

trait ObjectStorage { self =>
  def listBuckets(compartmentId: String, namespace: String): IO[BmcException, ObjectStorageBucketListing]

  def listObjects(namespace: String, bucketName: String): IO[BmcException, ObjectStorageObjectListing] =
    listObjects(namespace, bucketName, ListObjectsOptions.default)

  def listObjects(namespace: String, bucketName: String, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

  def listAllObjects(namespace: String, bucketName: String): Stream[BmcException, ObjectSummary] =
    listAllObjects(namespace, bucketName, ListObjectsOptions.default)

  def listAllObjects(namespace: String, bucketName: String, options: ListObjectsOptions): Stream[BmcException, ObjectSummary] =
    ZStream
      .fromZIO(self.listObjects(namespace, bucketName, options))
      .flatMap(listing => paginateObjects(listing, options).mapConcat(_.objectSummaries))

  def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): IO[BmcException, ObjectStorageObjectListing]

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): Stream[BmcException, ObjectStorageObjectListing] =
    ZStream.paginateZIO(initialListing) {
      case current @ ObjectStorageObjectListing(_, _, _, None) => ZIO.succeed(current -> None)
      case current                                             => self.getNextObjects(current, options).map(next => current -> Some(next))
    }

  def getObject(namespace: String, bucketName: String, name: String): Stream[BmcException, Byte] =
    getObject(namespace, bucketName, name, GetObjectOptions.default)

  def getObject(namespace: String, bucketName: String, name: String, options: GetObjectOptions): Stream[BmcException, Byte]
}

object ObjectStorage {
  def live(settings: ObjectStorageSettings): Layer[ConnectionError, ObjectStorage] =
    ZLayer.scoped(Live.connect(settings))
}
