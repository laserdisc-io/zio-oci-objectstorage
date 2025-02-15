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

package zio.oci

import com.oracle.bmc.model.BmcException
import zio.ZIO
import zio.stream.ZStream

package object objectstorage {
  type ObjectStorageStream[A] = ZStream[ObjectStorage, BmcException, A]

  def listBuckets(compartmentId: String, namespace: String): ZIO[ObjectStorage, BmcException, ObjectStorageBucketListing] =
    ZIO.environmentWithZIO(_.get.listBuckets(compartmentId, namespace))

  def listObjects(namespace: String, bucket: String): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.listObjects(namespace, bucket))

  def listObjects(namespace: String, bucket: String, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.listObjects(namespace, bucket, options))

  def listAllObjects(namespace: String, bucket: String) = ZStream.environmentWithStream[ObjectStorage](_.get.listAllObjects(namespace, bucket))

  def listAllObjects(namespace: String, bucket: String, options: ListObjectsOptions) =
    ZStream.environmentWithStream[ObjectStorage](_.get.listAllObjects(namespace, bucket, options))

  def getNextObjects(listing: ObjectStorageObjectListing, options: ListObjectsOptions): ZIO[ObjectStorage, BmcException, ObjectStorageObjectListing] =
    ZIO.environmentWithZIO(_.get.getNextObjects(listing, options))

  def getObject(namespace: String, bucket: String, name: String): ZStream[ObjectStorage, BmcException, Byte] =
    ZStream.environmentWithStream(_.get.getObject(namespace, bucket, name))

  def getObject(
      namespace: String,
      bucket: String,
      name: String,
      options: GetObjectOptions
  ): ZStream[ObjectStorage, BmcException, Byte] =
    ZStream.environmentWithStream(_.get.getObject(namespace, bucket, name, options))

  def paginateObjects(initialListing: ObjectStorageObjectListing, options: ListObjectsOptions): ObjectStorageStream[ObjectStorageObjectListing] =
    ZStream.environmentWithStream[ObjectStorage](_.get.paginateObjects(initialListing, options))
}
