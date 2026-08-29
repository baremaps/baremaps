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

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A dense map from {@code long} keys to fixed-size values, stored as a flat array addressed by the
 * key. It is the fastest map for dense keys, such as the ids of a full OpenStreetMap planet, and
 * wastes space for sparse ones.
 *
 * <p>
 * Being an array, the map has no notion of absent keys: every key below the allocated bound is
 * present and reads as the zero value of its type until it is written. {@link #size()} is that
 * bound, which grows a segment at a time.
 */
public class MemoryAlignedDataMap<E> implements DataMap<Long, E> {

  private final FixedSizeDataType<E> dataType;

  private final Memory memory;

  private final int valueShift;

  private final long maxKey;

  public MemoryAlignedDataMap(FixedSizeDataType<E> dataType, Memory memory) {
    int valueSize = dataType.size();
    if (valueSize <= 0 || (valueSize & -valueSize) != valueSize) {
      throw new DataCollectionException("The data type size must be a power of 2");
    }
    if (valueSize > memory.segmentSize()) {
      throw new DataCollectionException("The segment size is too small for the data type");
    }
    this.dataType = dataType;
    this.memory = memory;
    this.valueShift = Integer.numberOfTrailingZeros(valueSize);
    // Segments are indexed by an int, which bounds the addressable keys.
    int segmentShift = Long.numberOfTrailingZeros(memory.segmentSize());
    this.maxKey = (1L << (Integer.SIZE - 1 + segmentShift - valueShift)) - 1;
  }

  private long position(Object key) {
    Objects.requireNonNull(key, "Key cannot be null");
    long k = (Long) key;
    if (k < 0 || k > maxKey) {
      throw new IndexOutOfBoundsException(
          "Key should be between 0 and " + maxKey + ", but was " + k);
    }
    return k << valueShift;
  }

  @Override
  public E put(Long key, E value) {
    Objects.requireNonNull(value, "Value cannot be null");
    long position = position(key);
    E previous = memory.read(dataType, position);
    memory.write(dataType, position, value);
    return previous;
  }

  @Override
  public E get(Object key) {
    return memory.read(dataType, position(key));
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof Long k && k >= 0 && k < size();
  }

  @Override
  public boolean containsValue(Object value) {
    Iterator<E> iterator = valueIterator();
    while (iterator.hasNext()) {
      if (iterator.next().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public long size() {
    return memory.size() >> valueShift;
  }

  @Override
  public void clear() {
    try {
      memory.clear();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return new Iterator<>() {
      private final long size = size();
      private long key = 0;

      @Override
      public boolean hasNext() {
        return key < size;
      }

      @Override
      public Entry<Long, E> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        long k = key++;
        return Map.entry(k, get(k));
      }
    };
  }

  @Override
  public void close() {
    memory.close();
  }
}
