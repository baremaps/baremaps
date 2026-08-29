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

import com.baremaps.data.type.DataType;
import com.baremaps.data.type.FixedSizeDataType;
import com.baremaps.data.type.LongDataType;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

/**
 * A map whose keys are inserted in increasing order, as the ids of an OpenStreetMap file are. The
 * keys are kept in a sorted list and the values in a parallel list, so that nothing is wasted on
 * sparse keys and a lookup costs a binary search.
 *
 * <p>
 * To keep that search short, the keys are grouped in chunks of 256 consecutive values and a third
 * list records the index of the first key of each chunk. A lookup only searches the chunk of its
 * key, which is at most 256 entries, whatever the size of the map.
 *
 * <p>
 * Keys must be distinct and increasing, {@link #put(Long, Object)} rejects any other key.
 */
public class MonotonicDataMap<E> implements DataMap<Long, E> {

  private static final int CHUNK_SHIFT = 8;

  private final DataList<Long> offsets;

  private final DataList<Long> keys;

  private final DataList<E> values;

  // The last key inserted, or -1 for an empty map; keeps put from re-reading the keys.
  private long lastKey;

  /** Creates a map of fixed-size values in off-heap memory. */
  public MonotonicDataMap(FixedSizeDataType<E> dataType) {
    this(new MemoryAlignedDataList<>(dataType));
  }

  /** Creates a map of variable-size values in off-heap memory. */
  public MonotonicDataMap(DataType<E> dataType) {
    this(new IndexedDataList<>(dataType));
  }

  /** Creates a map over the given values, with keys and offsets in off-heap memory. */
  public MonotonicDataMap(DataList<E> values) {
    this(new MemoryAlignedDataList<>(new LongDataType()),
        new MemoryAlignedDataList<>(new LongDataType()), values);
  }

  /**
   * Creates a map over the given lists, which must either all be empty or come from the same
   * closed map.
   */
  public MonotonicDataMap(DataList<Long> offsets, DataList<Long> keys, DataList<E> values) {
    if (keys.size() != values.size()) {
      throw new DataCollectionException("The keys and values have different sizes");
    }
    this.offsets = offsets;
    this.keys = keys;
    this.values = values;
    this.lastKey = keys.isEmpty() ? -1 : keys.get(keys.size() - 1);
  }

  @Override
  public synchronized E put(Long key, E value) {
    if (key <= lastKey) {
      throw new IllegalArgumentException(
          "Keys must be increasing, but " + key + " follows " + lastKey);
    }
    long index = keys.size();
    long chunk = key >>> CHUNK_SHIFT;
    // Chunks without keys point to the first key of the next chunk.
    while (offsets.size() <= chunk) {
      offsets.add(index);
    }
    keys.add(key);
    values.add(value);
    lastKey = key;
    return null;
  }

  private long indexOf(Object keyObject) {
    if (!(keyObject instanceof Long key) || key < 0) {
      return -1;
    }
    long chunk = key >>> CHUNK_SHIFT;
    if (chunk >= offsets.size()) {
      return -1;
    }
    long lo = offsets.get(chunk);
    long hi = (chunk + 1 < offsets.size() ? offsets.get(chunk + 1) : keys.size()) - 1;
    while (lo <= hi) {
      long mid = (lo + hi) >>> 1;
      long value = keys.get(mid);
      if (value < key) {
        lo = mid + 1;
      } else if (value > key) {
        hi = mid - 1;
      } else {
        return mid;
      }
    }
    return -1;
  }

  @Override
  public E get(Object key) {
    long index = indexOf(key);
    return index < 0 ? null : values.get(index);
  }

  @Override
  public boolean containsKey(Object key) {
    return indexOf(key) >= 0;
  }

  @Override
  public boolean containsValue(Object value) {
    return values.contains(value);
  }

  @Override
  public long size() {
    return keys.size();
  }

  @Override
  public Iterator<Long> keyIterator() {
    return keys.iterator();
  }

  @Override
  public Iterator<E> valueIterator() {
    return values.iterator();
  }

  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return new Iterator<>() {
      private final long size = size();
      private long index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public Entry<Long, E> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        long i = index++;
        return Map.entry(keys.get(i), values.get(i));
      }
    };
  }

  @Override
  public synchronized void clear() {
    offsets.clear();
    keys.clear();
    values.clear();
    lastKey = -1;
  }

  @Override
  public void close() throws Exception {
    offsets.close();
    keys.close();
    values.close();
  }
}
