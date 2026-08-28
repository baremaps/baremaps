/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.data.collection;



import com.google.common.collect.Streams;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import com.baremaps.data.type.LongDataType;

/**
 * A map that stores fixed-size values in monotonically increasing key order.
 * Optimized for sequential insertions and fast binary search lookups.
 *
 * <p>
 * This code has been adapted from Planetiler (Apache license).
 *
 * <p>
 * Copyright (c) Planetiler.
 */
public class MonotonicFixedSizeDataMap<E> implements DataMap<Long, E> {

  private final DataList<Long> offsets;
  private final DataList<Long> keys;
  private final DataList<E> values;
  private long lastChunk = -1;

  /**
   * Static factory method to create a new builder.
   *
   * @param <E> the type of elements
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for {@link MonotonicFixedSizeDataMap}.
   *
   * @param <E> the type of elements
   */
  public static class Builder<E> {
    private DataList<Long> offsets;
    private DataList<Long> keys;
    private DataList<E> values;

    /**
     * Sets the offsets for the map.
     *
     * @param offsets the list of offsets
     * @return this builder
     */
    public Builder<E> offsets(DataList<Long> offsets) {
      this.offsets = offsets;
      return this;
    }

    /**
     * Sets the keys for the map.
     *
     * @param keys the list of keys
     * @return this builder
     */
    public Builder<E> keys(DataList<Long> keys) {
      this.keys = keys;
      return this;
    }

    /**
     * Sets the values for the map.
     *
     * @param values the list of values
     * @return this builder
     */
    public Builder<E> values(DataList<E> values) {
      this.values = values;
      return this;
    }

    /**
     * Builds a new {@link MonotonicFixedSizeDataMap}.
     *
     * @return a new MonotonicFixedSizeDataMap
     * @throws IllegalStateException if values are missing
     */
    public MonotonicFixedSizeDataMap<E> build() {
      if (values == null) {
        throw new IllegalStateException("Values must be specified");
      }
      
      if (offsets == null) {
        offsets = MemoryAlignedDataList.<Long>builder()
            .dataType(new LongDataType())
            .build();
      }
      
      if (keys == null) {
        keys = MemoryAlignedDataList.<Long>builder()
            .dataType(new LongDataType())
            .build();
      }
      
      return new MonotonicFixedSizeDataMap<>(offsets, keys, values);
    }
  }

  /**
   * Private constructor for {@link MonotonicFixedSizeDataMap}.
   *
   * @param offsets the list of offsets
   * @param keys the list of keys
   * @param values the list of values
   */
  private MonotonicFixedSizeDataMap(
      DataList<Long> offsets,
      DataList<Long> keys,
      DataList<E> values) {
    this.offsets = offsets;
    this.keys = keys;
    this.values = values;
  }

  /** {@inheritDoc} */
  public E get(Object keyObject) {
    long key = (long) keyObject;
    long chunk = key >>> 8;
    if (chunk >= offsets.size()) {
      return null;
    }
    long lo = offsets.get(chunk);
    long hi = Math.min(keys.size(),
        chunk >= offsets.size() - 1 ? keys.size() : offsets.get(chunk + 1)) - 1;
    while (lo <= hi) {
      long index = (lo + hi) >>> 1;
      long value = keys.get(index);
      if (value < key) {
        lo = index + 1;
      } else if (value > key) {
        hi = index - 1;
      } else {
        return values.get(index);
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  public E put(Long key, E value) {
    long size = keys.size();
    long chunk = key >>> 8;
    if (chunk != lastChunk) {
      while (offsets.size() <= chunk) {
        offsets.add(size);
      }
      lastChunk = chunk;
    }
    keys.add(key);
    values.add(value);
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return keys.size();
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object key) {
    return keys.contains(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsValue(Object value) {
    return values.contains(value);
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Long> keyIterator() {
    return keys.iterator();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<E> valueIterator() {
    return values.iterator();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return Streams.zip(
        Streams.stream(keyIterator()),
        Streams.stream(valueIterator()),
        Map::entry).iterator();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    offsets.clear();
    keys.clear();
    values.clear();
  }

  @Override
  public void close() throws Exception {
    try {
      offsets.close();
      keys.close();
      values.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
