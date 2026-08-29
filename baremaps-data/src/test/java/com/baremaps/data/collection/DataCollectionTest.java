/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.data.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.type.LongDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The default methods of {@link DataCollection} and the collection views. */
class DataCollectionTest {

  @Test
  void defaults() {
    var collection = new AppendOnlyLog<>(new LongDataType());
    assertTrue(collection.isEmpty());
    assertTrue(collection.addAll(List.of(1L, 2L, 3L)));
    assertFalse(collection.addAll(List.of()));
    assertEquals(3, collection.size());
    assertTrue(collection.contains(2L));
    assertFalse(collection.contains(4L));
    assertFalse(collection.contains("2"));
    assertTrue(collection.containsAll(Set.of(1L, 3L)));
    assertFalse(collection.containsAll(Set.of(1L, 4L)));
    assertEquals(6L, collection.parallelStream().mapToLong(Long::longValue).sum());
    assertEquals(List.of(1L, 2L, 3L), collection.stream().toList());
  }

  @Test
  void readOnlyByDefault() {
    DataCollection<Long> readOnly = new DataCollection<>() {
      @Override
      public long size() {
        return 0;
      }

      @Override
      public java.util.Iterator<Long> iterator() {
        return List.<Long>of().iterator();
      }

      @Override
      public void clear() {}

      @Override
      public void close() {}
    };
    assertThrows(UnsupportedOperationException.class, () -> readOnly.add(1L));
  }

  @Test
  void collectionViews() throws Exception {
    var collection = new AppendOnlyLog<>(new LongDataType());
    var view = DataConversions.asCollection(collection);
    assertTrue(view.add(1L));
    assertEquals(1, view.size());
    assertEquals(List.of(1L), new ArrayList<>(view));
    assertSame(collection, DataConversions.asDataCollection(view));
    view.clear();
    assertTrue(collection.isEmpty());

    var list = new ArrayList<Long>(List.of(1L));
    var dataCollection = DataConversions.asDataCollection(list);
    assertEquals(1, dataCollection.size());
    assertTrue(dataCollection.add(2L));
    assertEquals(List.of(1L, 2L), dataCollection.stream().toList());
    assertSame(list, DataConversions.asCollection(dataCollection));
    dataCollection.clear();
    dataCollection.close();
    assertTrue(list.isEmpty());
  }

  @Test
  void exceptions() {
    var cause = new RuntimeException("cause");
    assertEquals("message", new DataCollectionException("message").getMessage());
    assertSame(cause, new DataCollectionException(cause).getCause());
    assertSame(cause, new DataCollectionException("message", cause).getCause());
    assertEquals("message", new com.baremaps.data.memory.MemoryException("message").getMessage());
    assertSame(cause, new com.baremaps.data.memory.MemoryException("message", cause).getCause());
  }
}
