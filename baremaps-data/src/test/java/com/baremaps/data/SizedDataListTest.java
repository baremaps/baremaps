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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baremaps.data.collection.DataCollectionException;
import com.baremaps.data.collection.FixedSizeDataList;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.memory.OffHeapMemory;
import com.baremaps.data.type.LongDataType;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SizedDataListTest {

  @Test
  void segmentsTooSmall() {
    var dataType = new LongDataType();
    var memory = new OffHeapMemory(4);
    assertThrows(DataCollectionException.class, () -> new FixedSizeDataList<>(dataType, memory));
  }

  @ParameterizedTest
  @MethodSource("com.baremaps.data.memory.MemoryProvider#memories")
  void appendFixedSizeValues(Memory memory) throws IOException {
    var list = new FixedSizeDataList<>(new LongDataType(), memory);
    for (int i = 0; i < 1 << 10; i++) {
      assertEquals(i, list.addIndexed((long) i));
    }
    for (int i = 0; i < 1 << 10; i++) {
      assertEquals(i, list.get(i));
    }
    memory.close();
    memory.clear();
  }
}
