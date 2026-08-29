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
import static org.junit.jupiter.api.Assertions.assertSame;

import com.baremaps.data.type.LongDataType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The views are live and unwrap to the original. */
class DataConversionsTest {

  @Test
  void listViewIsLive() {
    var list = new MemoryAlignedDataList<>(new LongDataType());
    List<Long> view = DataConversions.asList(list);
    assertEquals(0, view.size());
    list.add(1L);
    assertEquals(1, view.size());
    view.add(2L);
    assertEquals(2, list.size());
    assertEquals(List.of(1L, 2L), view);
    assertSame(list, DataConversions.asDataList(view));
  }

  @Test
  void mapViewIsLive() {
    var map = new IndexedDataMap<>(new LongDataType());
    var view = DataConversions.asMap(map);
    map.put(1L, 10L);
    assertEquals(1, view.size());
    assertEquals(10L, view.get(1L));
    view.put(2L, 20L);
    assertEquals(20L, map.get(2L));
    assertEquals(java.util.Map.of(1L, 10L, 2L, 20L), view);
    assertSame(map, DataConversions.asDataMap(view));
  }

  @Test
  void heapCollectionsAsDataCollections() throws Exception {
    var list = new ArrayList<Long>();
    DataList<Long> dataList = DataConversions.asDataList(list);
    assertEquals(0, dataList.addIndexed(1L));
    assertEquals(List.of(1L), list);
    assertSame(list, DataConversions.asList(dataList));

    var map = new HashMap<Long, Long>();
    DataMap<Long, Long> dataMap = DataConversions.asDataMap(map);
    dataMap.put(1L, 2L);
    assertEquals(2L, map.get(1L));
    assertEquals(1, dataMap.keys().iterator().next());
  }
}
