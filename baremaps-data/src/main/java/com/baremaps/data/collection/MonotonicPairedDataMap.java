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



import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.PairDataType.Pair;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map that stores values as key-value pairs in monotonically increasing key order. Optimized for
 * sequential insertions and fast binary search lookups.
 *
 * <p>
 * This code has been adapted from Planetiler (Apache license).
 *
 * <p>
 * Copyright (c) Planetiler.
 */
public class MonotonicPairedDataMap<E> implements DataMap<Long, E> {

  private final DataList<Long> offsets;
  private final MemoryAlignedDataList<Pair<Long, E>> values;

  private long lastChunk = -1;

  /**
   * Creates a new builder for a MonotonicPairedDataMap.
   *
   * @param <E> the type of values
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for MonotonicPairedDataMap.
   *
   * @param <E> the type of values
   */
  public static class Builder<E> {
    private DataList<Long> offsets;
    private MemoryAlignedDataList<Pair<Long, E>> values;

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
     * Sets the values for the map.
     *
     * @param values the buffer of values
     * @return this builder
     */
    public Builder<E> values(MemoryAlignedDataList<Pair<Long, E>> values) {
      this.values = values;
      return this;
    }

    /**
     * Builds a new MonotonicPairedDataMap.
     *
     * @return a new MonotonicPairedDataMap
     * @throws IllegalStateException if values are missing
     */
    public MonotonicPairedDataMap<E> build() {
      if (values == null) {
        throw new IllegalStateException("Values must be specified");
      }

      if (offsets == null) {
        offsets = MemoryAlignedDataList.<Long>builder()
            .dataType(new LongDataType())
            .build();
      }

      return new MonotonicPairedDataMap<>(offsets, values);
    }
  }

  /**
   * Constructs a MonotonicPairedDataMap.
   *
   * @param offsets the list of offsets
   * @param values the buffer of values
   */
  private MonotonicPairedDataMap(DataList<Long> offsets,
      MemoryAlignedDataList<Pair<Long, E>> values) {
    this.offsets = offsets;
    this.values = values;
  }

  /** {@inheritDoc} */
  public E put(Long key, E value) {
    long index = values.size();
    long chunk = key >>> 8;
    if (chunk != lastChunk) {
      while (offsets.size() <= chunk) {
        offsets.add(index);
      }
      lastChunk = chunk;
    }
    values.add(new Pair<>(key, value));
    return null;
  }

  /** {@inheritDoc} */
  public E get(Object keyObject) {
    long key = (long) keyObject;
    long chunk = key >>> 8;
    if (chunk >= offsets.size()) {
      return null;
    }
    long lo = offsets.get(chunk);
    long hi =
        Math.min(
            values.size(),
            chunk >= offsets.size() - 1
                ? values.size()
                : offsets.get(chunk + 1))
            - 1;
    while (lo <= hi) {
      long index = (lo + hi) >>> 1;
      Pair<Long, E> pair = values.get(index);
      long value = pair.left();
      if (value < key) {
        lo = index + 1;
      } else if (value > key) {
        hi = index - 1;
      } else {
        // found
        return pair.right();
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Long> keyIterator() {
    return values.stream().map(Pair::left).iterator();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<E> valueIterator() {
    return values.stream().map(Pair::right).iterator();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return values.stream()
        .map(p -> Map.entry(p.left(), p.right()))
        .iterator();
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return values.size();
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsValue(Object value) {
    return values.stream().anyMatch(p -> p.right().equals(value));
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    offsets.clear();
    values.clear();
  }

  @Override
  public void close() throws Exception {
    try {
      offsets.close();
      values.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
