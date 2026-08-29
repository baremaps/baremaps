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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.ByteArrayDataType;
import com.baremaps.data.type.LongDataType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Values around segment boundaries: fitting exactly, one byte short, and larger than a segment. */
class AppendOnlyLogBoundaryTest {

  private static final int SEGMENT = 64;

  private static byte[] bytes(int length) {
    var value = new byte[length];
    for (int i = 0; i < length; i++) {
      value[i] = (byte) (length + i);
    }
    return value;
  }

  /** Byte arrays are encoded as a 4-byte size followed by the bytes. */
  private static int encoded(int length) {
    return Integer.BYTES + length;
  }

  @Test
  void exactFitStaysInSegment() {
    var log = new AppendOnlyLog<>(new ByteArrayDataType(), Memory.offHeap(SEGMENT));
    assertEquals(0, log.addPositioned(bytes(SEGMENT - encoded(0))));
    assertEquals(SEGMENT, log.addPositioned(bytes(1)));
  }

  @Test
  void oneByteShortStartsNextSegment() {
    var log = new AppendOnlyLog<>(new ByteArrayDataType(), Memory.offHeap(SEGMENT));
    int first = SEGMENT - encoded(0) - 1;
    assertEquals(0, log.addPositioned(bytes(first)));
    // 1 byte left: the next 5-byte value cannot fit, it goes to the next segment.
    assertEquals(SEGMENT, log.addPositioned(bytes(1)));
    assertEquals(SEGMENT + encoded(1), log.addPositioned(bytes(1)));
  }

  @Test
  void tailTooSmallForASizeHeader() {
    var log = new AppendOnlyLog<>(new ByteArrayDataType(), Memory.offHeap(SEGMENT));
    // Leave 3 bytes: not even a size fits, the iterator must skip the tail without reading it.
    log.add(bytes(SEGMENT - encoded(0) - 3));
    log.add(bytes(0));
    log.add(bytes(2));
    var read = new ArrayList<byte[]>();
    log.forEach(read::add);
    assertEquals(3, read.size());
    assertArrayEquals(bytes(0), read.get(1));
    assertArrayEquals(bytes(2), read.get(2));
  }

  @Test
  void largerThanSegmentIsRejected() {
    var log = new AppendOnlyLog<>(new ByteArrayDataType(), Memory.offHeap(SEGMENT));
    assertThrows(DataCollectionException.class, () -> log.add(bytes(SEGMENT)));
    assertEquals(0, log.size());
  }

  @Test
  void iterationMatchesPositionsAcrossManyBoundaries() {
    var log = new AppendOnlyLog<>(new ByteArrayDataType(), Memory.offHeap(SEGMENT));
    List<byte[]> values = new ArrayList<>();
    List<Long> positions = new ArrayList<>();
    for (int i = 0; i < 2000; i++) {
      var value = bytes(i % (SEGMENT - encoded(0)));
      values.add(value);
      positions.add(log.addPositioned(value));
    }
    int i = 0;
    for (byte[] value : log) {
      assertArrayEquals(values.get(i), value);
      assertArrayEquals(values.get(i), log.getPositioned(positions.get(i)));
      i++;
    }
    assertEquals(values.size(), i);
  }

  @Test
  void geometriesAcrossBoundaries() {
    // A geometry's size is read from a tag byte, so a zero-filled tail must read as "nothing".
    var factory = new org.locationtech.jts.geom.GeometryFactory();
    var log = new AppendOnlyLog<>(new com.baremaps.data.type.GeometryDataType(factory),
        Memory.offHeap(128));
    var values = new ArrayList<org.locationtech.jts.geom.Geometry>();
    for (int i = 0; i < 500; i++) {
      var coordinates = new org.locationtech.jts.geom.Coordinate[1 + i % 5];
      for (int j = 0; j < coordinates.length; j++) {
        coordinates[j] = new org.locationtech.jts.geom.Coordinate(i, j);
      }
      values.add(coordinates.length == 1
          ? factory.createPoint(coordinates[0])
          : factory.createLineString(coordinates));
    }
    values.forEach(log::add);
    int i = 0;
    for (var value : log) {
      assertEquals(values.get(i++), value);
    }
    assertEquals(values.size(), i);
  }

  @Test
  void fixedSizeValuesNeverStraddle() {
    // 8-byte values in 64-byte segments with a 24-byte one first: 5 more fit, the 7th moves on.
    var log = new AppendOnlyLog<>(new LongDataType(), Memory.offHeap(SEGMENT));
    for (int i = 0; i < 100; i++) {
      long position = log.addPositioned((long) i);
      assertEquals(position / SEGMENT, (position + Long.BYTES - 1) / SEGMENT);
    }
    long expected = 0;
    for (long value : log) {
      assertEquals(expected++, value);
    }
  }
}
