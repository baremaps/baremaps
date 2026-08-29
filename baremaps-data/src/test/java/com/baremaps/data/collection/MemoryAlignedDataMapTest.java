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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.memory.OnHeapMemory;
import com.baremaps.data.type.IntegerDataType;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryAlignedDataMapTest {

  private MemoryAlignedDataMap<Integer> map;

  @BeforeEach
  void setUp() {
    map = new MemoryAlignedDataMap<>(new IntegerDataType(), new OnHeapMemory(1024));
  }

  @AfterEach
  void tearDown() {
    map = null;
  }

  @Test
  void put() {
    map.put(1L, 1);
    map.put(1023L, 2);
    map.put(1024L, 3);
    map.put(1025L, 4);
    map.put(0L, 5);
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(549755813888L, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(Long.MAX_VALUE, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(Long.MIN_VALUE >> 2, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(Long.MIN_VALUE, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(0xc000000000000000L, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(-1L, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(-2L, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(-1L << 60, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> map.put(-1L << 59, 1));

    assertThrows(NullPointerException.class, () -> map.put(null, 1));
    assertThrows(NullPointerException.class, () -> map.put(1L, null));
  }

  @Test
  void get() {
    map.put(1L, 1);
    map.put(2L, 2);
    assertEquals(1, map.get(1L));
    assertEquals(2, map.get(2L));
    assertEquals(0, map.get(3L));
    assertThrows(IndexOutOfBoundsException.class, () -> map.get(-1L));
    assertThrows(IndexOutOfBoundsException.class, () -> map.get(549755813888L));

    assertThrows(NullPointerException.class, () -> map.get(null));
  }

  @Test
  void containsKey() {
    assertFalse(map.containsKey(1));
    assertFalse(map.containsKey(1L));
    map.put(1L, 1);
    assertFalse(map.containsKey(1));
    assertTrue(map.containsKey(0L));
    assertTrue(map.containsKey(1L));
    assertTrue(map.containsKey(5L));
    assertFalse(map.containsKey(256L));

    map.put(500L, 1);
    assertTrue(map.containsKey(257L));
    assertTrue(map.containsKey(258L));
    assertTrue(map.containsKey(511L));

    assertFalse(map.containsKey(null));
  }

  @Test
  void containsValue() {
    assertFalse(map.containsValue(0));
    assertFalse(map.containsValue(1));
    map.put(1L, 1);
    assertTrue(map.containsValue(0));
    assertTrue(map.containsValue(1));
    assertFalse(map.containsValue(2));
    assertFalse(map.containsValue(255));
    map.put(256L, 1);
    assertFalse(map.containsValue(256));

    assertFalse(map.containsValue(null));
  }

  @Test
  void size() {
    assertEquals(0, map.size());
    map.put(0L, 1);
    map.put(1L, 1);
    assertEquals(256, map.size());
    map.put(256L, 1);
    assertEquals(512, map.size());
    map.put(600L, 6);
    assertEquals(768, map.size());
  }

  @Test
  void clear() {
    map.put(1L, 1);
    map.clear();
    assertEquals(0, map.size());
    assertFalse(map.containsKey(1L));
  }

  @Test
  void keyIterator() {
    Iterator<Long> itr1 = map.keyIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    map.put(1L, 1);
    Iterator<Long> itr2 = map.keyIterator();
    assertTrue(itr2.hasNext());
    for (long i = 0; i < 256; i++) {
      assertEquals(i, itr2.next());
    }
  }

  @Test
  void valueIterator() {
    Iterator<Integer> itr1 = map.valueIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    map.put(1L, 2);
    Iterator<Integer> itr2 = map.valueIterator();
    assertTrue(itr2.hasNext());
    for (int i = 0; i < 256; i++) {
      if (i == 1) {
        assertEquals(2, itr2.next());
      } else {
        assertEquals(0, itr2.next());
      }
    }
  }

  @Test
  void entryIterator() {
    Iterator<Map.Entry<Long, Integer>> itr1 = map.entryIterator();
    assertFalse(itr1.hasNext());
    assertThrows(NoSuchElementException.class, itr1::next);

    map.put(1L, 3);
    Iterator<Map.Entry<Long, Integer>> itr2 = map.entryIterator();
    assertTrue(itr2.hasNext());
    for (long i = 0; i < 256; i++) {
      if (i == 1) {
        assertEquals(Map.entry(i, 3), itr2.next());
      } else {
        assertEquals(Map.entry(i, 0), itr2.next());
      }
    }
  }
}
