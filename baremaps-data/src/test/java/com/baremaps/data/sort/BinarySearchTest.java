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

package com.baremaps.data.sort;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.data.algorithm.BinarySearch;
import com.baremaps.data.collection.MemoryAlignedDataList;
import com.baremaps.data.type.LongDataType;
import org.junit.jupiter.api.Test;

class BinarySearchTest {

  @Test
  void binarySearch() {
    var list = new MemoryAlignedDataList<>(new LongDataType());
    for (long i = 0; i < 1000; i++) {
      list.add(i);
    }
    for (long i = 0; i < 1000; i++) {
      assertEquals(i, BinarySearch.binarySearch(list, i, Long::compare));
    }
  }
}
