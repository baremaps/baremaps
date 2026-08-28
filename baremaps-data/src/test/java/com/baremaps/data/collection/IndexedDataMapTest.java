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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.type.IntegerDataType;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexedDataMapTest {

  private IndexedDataMap<Integer> map;

  @BeforeEach
  void setUp() {
    map = IndexedDataMap.<Integer>builder()
        .values(AppendOnlyLog.<Integer>builder()
            .dataType(new IntegerDataType())
            .build())
        .build();
  }

  @AfterEach
  void tearDown() {
    map.clear();
    map = null;
  }

  @Test
  void put() {
    map.put(-1L, 1);
    map.put(0L, 1);
    map.put(1L, 1);
    map.put(Long.MAX_VALUE, 1);
    map.put(Long.MIN_VALUE, 1);
    map.put(null, 1);

    assertThrows(NullPointerException.class, () -> map.put(256L, null));
  }

  @Test
  void get() {
    map.put(1L, 1);
    map.put(2L, 2);
    assertEquals(2, map.get(2L));
    assertEquals(1, map.get(1L));
    map.put(2L, 3);
    assertEquals(3, map.get(2L));
    assertNull(map.get(3L));
    assertNull(map.get(-1L));
    assertNull(map.get(549755813888L));
    assertNull(map.get(null));
    map.put(549755813888L, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, map.get(549755813888L));
    map.put(Long.MIN_VALUE, Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, map.get(Long.MIN_VALUE));
  }

  @Test
  void containsKey() {
    assertFalse(map.containsKey(1));
    assertFalse(map.containsKey(1L));
    map.put(1L, 1);
    assertFalse(map.containsKey(1));
    assertFalse(map.containsKey(0L));
    assertTrue(map.containsKey(1L));
    assertFalse(map.containsKey(5L));
    assertFalse(map.containsKey(256L));
    assertFalse(map.containsKey(null));
  }

  @Test
  void containsValue() {
    assertFalse(map.containsValue(0));
    assertFalse(map.containsValue(1));
    map.put(1L, 1);
    assertTrue(map.containsValue(1));
    assertFalse(map.containsValue(2));
    assertFalse(map.containsValue(255));
    map.put(256L, 1);
    assertFalse(map.containsValue(256));

    assertThrows(NullPointerException.class, () -> map.containsValue(null));
  }

  @Test
  void size() {
    assertEquals(0, map.size());
    map.put(0L, 1);
    map.put(1L, 1);
    assertEquals(2, map.size());
    map.put(1L, 2);
    assertEquals(2, map.size());
    map.put(256L, 1);
    assertEquals(3, map.size());
    map.put(600L, 6);
    assertEquals(4, map.size());
    map.put(549755813887L, 7);
    assertEquals(5, map.size());
  }

  @Test
  void clear() {
    assertTrue(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());

    map.put(1L, 1);
    assertEquals(1, map.size());
    map.put(324L, 1);
    assertEquals(2, map.size());
    map.clear();
    assertTrue(map.isEmpty());
  }

  @Test
  void keyIterator() {
    Iterator<Long> itr1 = map.keyIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    for (long i = 0; i < 256; i += 2) {
      map.put(i, 1);
    }
    Iterator<Long> itr2 = map.keyIterator();
    assertTrue(itr2.hasNext());
    for (long i = 0; i < 256; i += 2) {
      assertEquals(i, itr2.next());
    }
  }

  @Test
  void valueIterator() {
    Iterator<Integer> itr1 = map.valueIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    for (long i = 0; i < 256; i += 2) {
      map.put(i, (int) (i - 1));
    }
    Iterator<Integer> itr2 = map.valueIterator();
    assertTrue(itr2.hasNext());
    for (long i = 0; i < 256; i += 2) {
      assertEquals((int) (i - 1), itr2.next());
    }
  }

  @Test
  void entryIterator() {
    Iterator<Map.Entry<Long, Integer>> itr1 = map.entryIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    for (long i = 0; i < 256; i += 2) {
      map.put(i, (int) (i - 1));
    }
    Iterator<Map.Entry<Long, Integer>> itr2 = map.entryIterator();
    assertTrue(itr2.hasNext());
    for (long i = 0; i < 256; i += 2) {
      assertEquals(Map.entry(i, (int) (i - 1)), itr2.next());
    }
  }

  @Test
  void isEmpty() {
    assertTrue(map.isEmpty());
    map.put(1L, 1);
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }
}
