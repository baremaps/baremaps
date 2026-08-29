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

package com.baremaps.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.collection.DataCollectionException;
import com.baremaps.data.memory.OffHeapMemory;
import com.baremaps.data.type.DataType;
import com.baremaps.data.type.IntegerDataType;
import com.baremaps.data.type.IntegerListDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AppendOnlyLogTest {

  @Test
  void addFixedSizeData() {
    var collection = new AppendOnlyLog<>(new IntegerDataType(), new OffHeapMemory(1 << 10));
    for (int i = 0; i < 1 << 20; i++) {
      assertEquals((i << 2), collection.addPositioned(i));
    }
    for (int i = 0; i < 1 << 20; i++) {
      assertEquals(i, collection.getPositioned((i << 2)));
    }
  }

  @Test
  void addVariableSizeValues() {
    var collection = new AppendOnlyLog<>(new IntegerListDataType(), new OffHeapMemory(1 << 10));
    var random = new Random(0);
    var positions = new ArrayList<Long>();
    var values = new ArrayList<ArrayList<Integer>>();
    for (int i = 0; i < 1 << 20; i++) {
      var size = random.nextInt(10);
      var value = new ArrayList<Integer>();
      for (int j = 0; j < size; j++) {
        value.add(random.nextInt(1 << 20));
      }
      positions.add(collection.addPositioned(value));
      values.add(value);
    }
    for (int i = 0; i < positions.size(); i++) {
      var value = collection.getPositioned(positions.get(i));
      assertEquals(values.get(i), value);
    }
  }

  @Test
  void valueLargerThanSegment() {
    var collection = new AppendOnlyLog<>(new IntegerListDataType(), new OffHeapMemory(1 << 4));
    assertThrows(DataCollectionException.class,
        () -> collection.add(new ArrayList<>(List.of(1, 2, 3, 4, 5))));
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.type.DataTypeProvider#dataTypes")
  void testAllDataTypes(DataType dataType, Object value) {
    var num = 1000;
    var collection = new AppendOnlyLog<>(dataType, new OffHeapMemory(1 << 22));

    // write values
    for (int i = 0; i < num; i++) {
      collection.addPositioned(value);
    }

    // read values
    int count = 0;
    for (var v : collection) {
      if (value instanceof byte[]) {
        assertArrayEquals((byte[]) value, (byte[]) v);
      } else if (value instanceof short[]) {
        assertArrayEquals((short[]) value, (short[]) v);
      } else if (value instanceof int[]) {
        assertArrayEquals((int[]) value, (int[]) v);
      } else if (value instanceof long[]) {
        assertArrayEquals((long[]) value, (long[]) v);
      } else if (value instanceof float[]) {
        assertArrayEquals((float[]) value, (float[]) v);
      } else if (value instanceof double[]) {
        assertArrayEquals((double[]) value, (double[]) v);
      } else if (value instanceof char[]) {
        assertArrayEquals((char[]) value, (char[]) v);
      } else if (value instanceof boolean[]) {
        assertArrayEquals((boolean[]) value, (boolean[]) v);
      } else {
        assertEquals(value, v);
      }
      count++;
    }

    // check count
    assertEquals(num, count);
  }
}
