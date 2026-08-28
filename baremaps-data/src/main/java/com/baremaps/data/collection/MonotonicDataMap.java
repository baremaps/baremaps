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



import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.PairDataType;
import com.baremaps.data.type.PairDataType.Pair;

/**
 * A map that stores values in a monotonically increasing key order. Optimized for 
 * sequential insertions and binary search lookups with chunked indexing.
 *
 * <p>
 * This code has been adapted from Planetiler (Apache license).
 */
public class MonotonicDataMap<E> implements DataMap<Long, E> {

  private final DataList<Long> offsets;
  private final DataList<Pair<Long, Long>> keys;
  private final AppendOnlyLog<E> values;

  private long lastChunk = -1;
  
  /**
   * Creates a new builder for a MonotonicDataMap.
   *
   * @param <E> the type of values in the map
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }
  
  /**
   * Builder for MonotonicDataMap.
   *
   * @param <E> the type of values in the map
   */
  public static class Builder<E> {
    private DataList<Long> offsets;
    private DataList<Pair<Long, Long>> keys;
    private AppendOnlyLog<E> values;
    
    /**
     * Sets the offsets list.
     *
     * @param offsets the list of offsets
     * @return this builder
     */
    public Builder<E> offsets(DataList<Long> offsets) {
      this.offsets = offsets;
      return this;
    }
    
    /**
     * Sets the keys list.
     *
     * @param keys the list of keys
     * @return this builder
     */
    public Builder<E> keys(DataList<Pair<Long, Long>> keys) {
      this.keys = keys;
      return this;
    }
    
    /**
     * Sets the values buffer.
     *
     * @param values the buffer of values
     * @return this builder
     */
    public Builder<E> values(AppendOnlyLog<E> values) {
      this.values = values;
      return this;
    }
    
    /**
     * Builds a new MonotonicDataMap.
     *
     * @return a new MonotonicDataMap
     * @throws IllegalArgumentException if the values buffer is not provided
     */
    public MonotonicDataMap<E> build() {
      if (values == null) {
        throw new IllegalArgumentException("Values buffer must be provided");
      }
      
      if (offsets == null) {
        offsets = MemoryAlignedDataList.<Long>builder()
            .dataType(new LongDataType())
            .build();
      }
      
      if (keys == null) {
        keys = MemoryAlignedDataList.<Pair<Long, Long>>builder()
            .dataType(new PairDataType<>(new LongDataType(), new LongDataType()))
            .build();
      }
      
      return new MonotonicDataMap<>(offsets, keys, values);
    }
  }

  /**
   * Constructs a MonotonicDataMap.
   *
   * @param offsets the list of offsets
   * @param keys the list of keys
   * @param values the buffer of values
   */
  private MonotonicDataMap(DataList<Long> offsets, DataList<Pair<Long, Long>> keys,
      AppendOnlyLog<E> values) {
    this.offsets = offsets;
    this.keys = keys;
    this.values = values;
  }

  /** {@inheritDoc} */
  public E put(Long key, E value) {
    long index = keys.size();
    long chunk = key >>> 8;
    if (chunk != lastChunk) {
      while (offsets.size() <= chunk) {
        offsets.add(index);
      }
      lastChunk = chunk;
    }
    long position = values.addPositioned(value);
    keys.add(new Pair<>(key, position));
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
            keys.size(),
            chunk >= offsets.size() - 1
                ? keys.size()
                : offsets.get(chunk + 1))
            - 1;
    while (lo <= hi) {
      long index = (lo + hi) >>> 1;
      Pair<Long, Long> pair = keys.get(index);
      long value = pair.left();
      if (value < key) {
        lo = index + 1;
      } else if (value > key) {
        hi = index - 1;
      } else {
        // found
        return values.getPositioned(pair.right());
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Long> keyIterator() {
    return keys.stream().map(Pair::left).iterator();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<E> valueIterator() {
    return keys.stream().map(Pair::right).map(values::getPositioned).iterator();
  }

  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return keys.stream()
        .map(p -> Map.entry(p.left(), values.getPositioned(p.right())))
        .iterator();
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return keys.size();
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsValue(Object value) {
    return values.contains(value);
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    offsets.clear();
    keys.clear();
    values.clear();
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    try {
      offsets.close();
      keys.close();
      values.close();
    } catch (Exception e) {
      throw new DataCollectionException(e);
    }
  }
}
