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



import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;

/**
 * A map that stores fixed-size memory-aligned elements with optimized memory addressing.
 * Uses bit-shift operations for fast memory access.
 *
 * <p>
 * This code has been adapted from Planetiler (Apache license).
 */
public class MemoryAlignedDataMap<E> implements DataMap<Long, E> {

  private final FixedSizeDataType<E> dataType;

  private final Memory<?> memory;

  private final int valueShift;

  private final long segmentShift;

  private final long segmentMask;

  private final long upperBoundary;

  /**
   * Creates a new builder for a MemoryAlignedDataMap.
   *
   * @param <E> the type of values
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for MemoryAlignedDataMap.
   *
   * @param <E> the type of values
   */
  public static class Builder<E> {
    private FixedSizeDataType<E> dataType;
    private Memory<?> memory;

    /**
     * Sets the data type for the map.
     *
     * @param dataType the data type
     * @return this builder
     */
    public Builder<E> dataType(FixedSizeDataType<E> dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Sets the memory for the map.
     *
     * @param memory the memory
     * @return this builder
     */
    public Builder<E> memory(Memory<?> memory) {
      this.memory = memory;
      return this;
    }

    /**
     * Builds a new MemoryAlignedDataMap.
     *
     * @return a new MemoryAlignedDataMap
     * @throws IllegalStateException if required parameters are missing
     */
    public MemoryAlignedDataMap<E> build() {
      if (dataType == null) {
        throw new IllegalStateException("Data type must be specified");
      }
      if (memory == null) {
        throw new IllegalStateException("Memory must be specified");
      }
      
      return new MemoryAlignedDataMap<>(dataType, memory);
    }
  }

  /**
   * Constructs a MemoryAlignedDataMap.
   *
   * @param dataType the data type
   * @param memory the memory
   * @throws DataCollectionException if memory and data type size requirements are not met
   * @throws IllegalArgumentException if data type size is not a power of 2
   */
  private MemoryAlignedDataMap(FixedSizeDataType<E> dataType, Memory<?> memory) {
    if (dataType.size() > memory.segmentSize()) {
      throw new DataCollectionException("The segment size is too small for the data type");
    }
    if ((dataType.size() & -dataType.size()) != dataType.size()) {
      throw new IllegalArgumentException("The data type size must be a fixed power of 2");
    }
    if (memory.segmentSize() % dataType.size() != 0) {
      throw new DataCollectionException("The segment size and data type size must be aligned");
    }
    this.dataType = dataType;
    this.memory = memory;
    this.valueShift = (int) (Math.log(dataType.size()) / Math.log(2));
    this.segmentShift = memory.segmentShift();
    this.segmentMask = memory.segmentMask();
    this.upperBoundary = segmentShift > 32 ? Long.MAX_VALUE >> valueShift
        : Long.MAX_VALUE >> (32 - segmentShift + valueShift);
  }

  /**
   * Checks if the key is within valid boundaries.
   *
   * @param key the key to check
   * @throws NullPointerException if key is null
   * @throws IndexOutOfBoundsException if key is outside valid range
   */
  private void checkBoundary(Long key) {
    Objects.requireNonNull(key, "Key couldn't be null");
    if (key < 0 || key > upperBoundary) {
      String msg =
          String.format("Key should between 0 and %d, but your key is %d", upperBoundary, key);
      throw new IndexOutOfBoundsException(msg);
    }
  }

  /** {@inheritDoc} */
  @Override
  public E put(Long key, E value) {
    checkBoundary(key);
    Objects.requireNonNull(value, "Value couldn't be null");
    long position = key << valueShift;
    int segmentIndex = (int) (position >>> segmentShift);
    int segmentOffset = (int) (position & segmentMask);
    ByteBuffer segment = memory.segment(segmentIndex);
    E previous = dataType.read(segment, segmentOffset);
    dataType.write(segment, segmentOffset, value);
    return previous;
  }

  /** {@inheritDoc} */
  @Override
  public E get(Object key) {
    checkBoundary((long) key);
    long position = (long) key << valueShift;
    int segmentIndex = (int) (position >>> segmentShift);
    int segmentOffset = (int) (position & segmentMask);
    ByteBuffer segment = memory.segment(segmentIndex);
    return dataType.read(segment, segmentOffset);
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object keyObject) {
    if (keyObject instanceof Long key) {
      return key >= 0 && key < size();
    } else {
      return false;
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public long size() {
    return memory.size() / dataType.size();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Long> keyIterator() {
    return new Iterator<>() {

      private final long size = size();

      private long index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public Long next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return index++;
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<E> valueIterator() {
    return new Iterator<>() {

      private final long size = size();

      private long index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return get(index++);
      }
    };
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
        long key = index++;
        return Map.entry(key, get(key));
      }
    };
  }

  @Override
  public void close() throws Exception {
    try {
      memory.close();
    } catch (Exception e) {
      throw new DataCollectionException(e);
    }
  }
}
