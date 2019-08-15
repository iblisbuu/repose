/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.hazelcast

import java.io
import java.util.concurrent.TimeUnit

import com.hazelcast.core.{HazelcastInstance, IMap}
import com.hazelcast.map.EntryProcessor
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyLong, anyString, eq => isEq}
import org.mockito.Mockito.{verify, when}
import org.openrepose.commons.test.MockitoAnswers
import org.openrepose.core.services.datastore.{DatastoreOperationException, Patch}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class HazelcastDatastoreTest
  extends FunSpec with BeforeAndAfterEach with MockitoSugar with MockitoAnswers with Matchers {

  final val TestKey: String = "testKey"

  var hazelcastInstance: HazelcastInstance = _
  var dataMap: IMap[String, io.Serializable] = _
  var hazelcastDatastore: HazelcastDatastore = _

  override protected def beforeEach(): Unit = {
    hazelcastInstance = mock[HazelcastInstance]
    dataMap = mock[IMap[String, io.Serializable]]

    when(hazelcastInstance.getMap[String, io.Serializable](any[String]))
      .thenReturn(dataMap)

    hazelcastDatastore = new HazelcastDatastore(hazelcastInstance)
  }

  describe("get") {
    it("should return null if nothing is mapped to the key") {
      val returned = hazelcastDatastore.get(TestKey)

      // An assertion is used rather than a ScalaTest matcher due to java.io.Serializable not being a
      // sub-type of AnyRef, which prevents ScalaTest matchers from performing a null check.
      assert(returned == null)
      verify(dataMap).get(TestKey)
    }

    it("should return the value mapped to the key") {
      val value = "value"

      when(dataMap.get(TestKey))
        .thenReturn(value, Nil: _*)

      val returned = hazelcastDatastore.get(TestKey)

      returned shouldEqual value
      verify(dataMap).get(TestKey)
    }

    it("should throw a DatastoreOperationException if the operation fails") {
      when(dataMap.get(TestKey))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy hazelcastDatastore.get(TestKey)
      verify(dataMap).get(TestKey)
    }
  }

  describe("put") {
    it("should store the value mapped to the key") {
      val value = "value"

      hazelcastDatastore.put(TestKey, value)

      verify(dataMap).set(isEq(TestKey), isEq(value), isEq(-1L), any[TimeUnit])
    }

    it("should store the value mapped to the key with a TTL") {
      val value = "value"
      val ttl = 10
      val timeUnit = TimeUnit.MINUTES

      hazelcastDatastore.put(TestKey, value, ttl, timeUnit)

      verify(dataMap).set(TestKey, value, ttl, timeUnit)
    }

    it("should throw a DatastoreOperationException if the operation fails") {
      val value = "value"

      when(dataMap.set(anyString, any[io.Serializable], anyLong, any[TimeUnit]))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy hazelcastDatastore.put(TestKey, value)
      verify(dataMap).set(isEq(TestKey), isEq(value), isEq(-1L), any[TimeUnit])
    }

    it("should throw a DatastoreOperationException if the operation fails (TTL)") {
      val value = "value"
      val ttl = 10
      val timeUnit = TimeUnit.MINUTES

      when(dataMap.set(anyString, any[io.Serializable], anyLong, any[TimeUnit]))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy hazelcastDatastore.put(TestKey, value, ttl, timeUnit)
      verify(dataMap).set(isEq(TestKey), isEq(value), isEq(ttl.longValue), isEq(timeUnit))
    }
  }

  describe("patch") {
    it("should execute a patch") {
      val patch = mock[Patch[String]]

      hazelcastDatastore.patch(TestKey, patch)

      verify(dataMap).executeOnKey(isEq(TestKey), any[EntryProcessor[_, _]])
    }

    it("should execute a patch (TTL)") {
      val patch = mock[Patch[String]]
      val ttl = 10
      val timeUnit = TimeUnit.MINUTES

      hazelcastDatastore.patch(TestKey, patch, ttl, timeUnit)

      verify(dataMap).executeOnKey(isEq(TestKey), any[EntryProcessor[_, _]])
    }

    it("should throw a DatastoreOperationException if the operation fails") {
      val patch = mock[Patch[String]]

      when(dataMap.executeOnKey(anyString, any[EntryProcessor[_, _]]))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy
        hazelcastDatastore.patch(TestKey, patch)
    }

    it("should throw a DatastoreOperationException if the operation fails (TTL)") {
      val patch = mock[Patch[String]]
      val ttl = 10
      val timeUnit = TimeUnit.MINUTES

      when(dataMap.executeOnKey(anyString, any[EntryProcessor[_, _]]))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy
        hazelcastDatastore.patch(TestKey, patch, ttl, timeUnit)
    }
  }

  describe("remove") {
    it("should return false if no mapping existed") {
      val returned = hazelcastDatastore.remove(TestKey)

      returned shouldBe false
      verify(dataMap).remove(TestKey)
    }

    it("should return true if a mapping was removed") {
      when(dataMap.remove(TestKey))
        .thenReturn("old value", Nil: _*)

      val returned = hazelcastDatastore.remove(TestKey)

      returned shouldBe true
      verify(dataMap).remove(TestKey)
    }

    it("should throw a DatastoreOperationException if the operation fails") {
      when(dataMap.remove(TestKey))
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy
        hazelcastDatastore.remove(TestKey)

      verify(dataMap).remove(TestKey)
    }
  }

  describe("removeAll") {
    it("should remove all data from the datastore") {
      hazelcastDatastore.removeAll()

      verify(dataMap).clear()
    }

    it("should throw a DatastoreOperationException if the operation fails") {
      when(dataMap.clear())
        .thenThrow(new RuntimeException("Failure!"))

      a[DatastoreOperationException] should be thrownBy hazelcastDatastore.removeAll()
      verify(dataMap).clear()
    }
  }

  describe("getName") {
    it("should return the name of the datastore") {
      hazelcastDatastore.getName shouldBe HazelcastDatastore.Name
    }
  }
}
