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
import com.baremaps.data.memory.OffHeapMemory;
import com.baremaps.data.type.FixedSizeDataType;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A list that stores fixed-size elements in memory. Elements can be stored in heap memory, off-heap
 * memory, or memory-mapped files.
 *
 * @param <E> The type of elements in the list
 */
public class FixedSizeDataList<E> implements DataList<E> {

  private final FixedSizeDataType<E> dataType;
  private final Memory<?> memory;
  private AtomicLong size;

  /**
   * Creates a new builder for a FixedSizeDataList.
   *
   * @param <E> the type of elements
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for FixedSizeDataList.
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
     * Builds a new FixedSizeDataList.
     *
     * @return a new FixedSizeDataList
     * @throws IllegalStateException if the data type is missing
     */
    public FixedSizeDataList<E> build() {
      if (dataType == null) {
        throw new IllegalStateException("Data type must be specified");
      }

      if (memory == null) {
        memory = new OffHeapMemory();
      }

      return new FixedSizeDataList<>(dataType, memory);
    }
  }

  /**
   * Constructs a FixedSizeDataList.
   *
   * @param dataType the data type
   * @param memory the memory
   * @throws DataCollectionException if the data type is too large for the memory segment size
   */
  private FixedSizeDataList(FixedSizeDataType<E> dataType, Memory<?> memory) {
    if (dataType.size() > memory.segmentSize()) {
      throw new DataCollectionException("The segment size is too small for the data type");
    }
    this.dataType = dataType;
    this.memory = memory;
    this.size = new AtomicLong(0);
  }

  /**
   * Writes an element at the specified index.
   *
   * @param index the index
   * @param value the element to write
   */
  private void write(long index, E value) {
    long position = index * dataType.size();
    int segmentIndex = (int) (position / memory.segmentSize());
    int segmentOffset = (int) (position % memory.segmentSize());
    ByteBuffer segment = memory.segment(segmentIndex);
    dataType.write(segment, segmentOffset, value);
  }

  /** {@inheritDoc} */
  @Override
  public long addIndexed(E value) {
    long index = size.getAndIncrement();
    write(index, value);
    return index;
  }

  /** {@inheritDoc} */
  @Override
  public void set(long index, E value) {
    if (index >= size()) {
      throw new IndexOutOfBoundsException();
    }
    write(index, value);
  }

  /** {@inheritDoc} */
  @Override
  public E get(long index) {
    long position = index * dataType.size();
    int segmentIndex = (int) (position / memory.segmentSize());
    int segmentOffset = (int) (position % memory.segmentSize());
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
