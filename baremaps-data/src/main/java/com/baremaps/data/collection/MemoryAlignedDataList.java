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
import com.baremaps.data.memory.MemoryException;
import com.baremaps.data.memory.OffHeapMemory;
import com.baremaps.data.type.FixedSizeDataType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A list that stores fixed-size memory-aligned elements. Optimized for performance with bit-shift
 * operations for memory addressing.
 *
 * @param <E> The type of elements in the list
 */
public class MemoryAlignedDataList<E> implements DataList<E> {

  private final FixedSizeDataType<E> dataType;
  private final Memory<?> memory;
  private final int valueShift;
  private final long segmentShift;
  private final long segmentMask;
  private AtomicLong size;

  /**
   * Creates a new builder for a MemoryAlignedDataList.
   *
   * @param <E> the type of elements
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for MemoryAlignedDataList.
   *
   * @param <E> the type of elements
   */
  public static class Builder<E> {
    private FixedSizeDataType<E> dataType;
    private Memory<?> memory;

    /**
     * Sets the data type for the list.
     *
     * @param dataType the data type
     * @return this builder
     */
    public Builder<E> dataType(FixedSizeDataType<E> dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Sets the memory for the list.
     *
     * @param memory the memory
     * @return this builder
     */
    public Builder<E> memory(Memory<?> memory) {
      this.memory = memory;
      return this;
    }

    /**
     * Builds a new MemoryAlignedDataList.
     *
     * @return a new MemoryAlignedDataList
     * @throws IllegalStateException if the data type is missing
     */
    public MemoryAlignedDataList<E> build() {
      if (dataType == null) {
        throw new IllegalStateException("Data type must be specified");
      }

      if (memory == null) {
        memory = new OffHeapMemory();
      }

      return new MemoryAlignedDataList<>(dataType, memory);
    }
  }

  /**
   * Constructs a MemoryAlignedDataList.
   *
   * @param dataType the data type
   * @param memory the memory
   * @throws DataCollectionException if memory and data type size requirements are not met
   * @throws IllegalArgumentException if data type size is not a power of 2
   */
  private MemoryAlignedDataList(FixedSizeDataType<E> dataType, Memory<?> memory) {
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
    this.size = new AtomicLong(0);
  }

  /**
   * Writes an element at the specified index.
   *
   * @param index the index
   * @param value the element to write
   */
  private void write(long index, E value) {
    long position = index << valueShift;
    int segmentIndex = (int) (position >>> segmentShift);
    int segmentOffset = (int) (position & segmentMask);
    ByteBuffer segment = memory.segment(segmentIndex);
    dataType.write(segment, segmentOffset, value);
  }

  /** {@inheritDoc} */
  public long addIndexed(E value) {
    long index = size.getAndIncrement();
    write(index, value);
    return index;
  }

  /** {@inheritDoc} */
  public void set(long index, E value) {
    if (index >= size.get()) {
      size.set(index + 1);
    }
    write(index, value);
  }

  /** {@inheritDoc} */
  public E get(long index) {
    long position = index << valueShift;
    int segmentIndex = (int) (position >> segmentShift);
    int segmentOffset = (int) (position & segmentMask);
    ByteBuffer segment = memory.segment(segmentIndex);
    return dataType.read(segment, segmentOffset);
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return size.get();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    size.set(0);
    try {
      memory.clear();
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }

  @Override
  public void close() throws Exception {
    try {
      memory.close();
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }
}
