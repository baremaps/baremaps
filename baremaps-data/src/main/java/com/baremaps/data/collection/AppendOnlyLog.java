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
import com.baremaps.data.type.DataType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A log of elements that can only be appended to. Elements are stored in memory and can be accessed
 * by their position. Append operations are thread-safe.
 *
 * @param <E> The type of elements in the log
 */
public class AppendOnlyLog<E> implements DataCollection<E> {

  private final DataType<E> dataType;
  private final Memory<?> memory;
  private final long segmentSize;
  private long offset;
  private long size;

  private final Lock lock = new ReentrantLock();

  /**
   * Creates a new builder for an AppendOnlyLog.
   *
   * @param <E> the type of elements
   * @return a new builder
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder for AppendOnlyLog.
   *
   * @param <E> the type of elements
   */
  public static class Builder<E> {
    private DataType<E> dataType;
    private Memory<?> memory;

    /**
     * Sets the data type for the log.
     *
     * @param dataType the data type
     * @return this builder
     */
    public Builder<E> dataType(DataType<?> dataType) {
      @SuppressWarnings("unchecked")
      DataType<E> castedDataType = (DataType<E>) dataType;
      this.dataType = castedDataType;
      return this;
    }

    /**
     * Sets the memory for the log.
     *
     * @param memory the memory
     * @return this builder
     */
    public Builder<E> memory(Memory<?> memory) {
      this.memory = memory;
      return this;
    }

    /**
     * Sets the memory for the log values.
     *
     * @param memory the memory
     * @return this builder
     */
    public Builder<E> values(Memory<?> memory) {
      return memory(memory);
    }

    /**
     * Builds a new AppendOnlyLog.
     *
     * @return a new AppendOnlyLog
     * @throws IllegalStateException if required parameters are missing
     */
    public AppendOnlyLog<E> build() {
      if (dataType == null) {
        throw new IllegalStateException("Data type must be specified");
      }

      if (memory == null) {
        memory = new OffHeapMemory();
      }

      return new AppendOnlyLog<>(dataType, memory);
    }
  }

  /**
   * Constructs an AppendOnlyLog.
   *
   * @param dataType the data type
   * @param memory the memory
   */
  private AppendOnlyLog(DataType<E> dataType, Memory<?> memory) {
    this.dataType = dataType;
    this.memory = memory;
    this.segmentSize = memory.segmentSize();
    this.size = memory.header().getLong(0);
    this.offset = 0;
  }

  /**
   * Persists the current size to memory.
   */
  public void persistSize() {
    memory.segment(0).putLong(0, size);
  }

  /**
   * Appends an element to the log and returns its position in memory.
   *
   * @param value the element to add
   * @return the position of the element in memory
   * @throws DataCollectionException if the element is too large for a segment
   */
  public long addPositioned(E value) {
    int valueSize = dataType.size(value);
    if (valueSize > segmentSize) {
      throw new DataCollectionException("The value is too big to fit in a segment");
    }

    lock.lock();
    long position = offset;
    long segmentIndex = position / segmentSize;
    long segmentOffset = position % segmentSize;

    if (segmentOffset + valueSize > segmentSize) {
      segmentOffset = 0;
      segmentIndex = segmentIndex + 1;
      position = segmentIndex * segmentSize;
    }
    offset = position + valueSize;
    this.size++;
    lock.unlock();

    ByteBuffer segment = memory.segment((int) segmentIndex);
    dataType.write(segment, (int) segmentOffset, value);

    return position;
  }

  /**
   * Returns the element at the specified position in memory.
   *
   * @param position the position of the element
   * @return the element
   */
  public E getPositioned(long position) {
    long segmentIndex = position / segmentSize;
    long segmentOffset = position % segmentSize;
    ByteBuffer buffer = memory.segment((int) segmentIndex);
    return dataType.read(buffer, (int) segmentOffset);
  }

  /** {@inheritDoc} */
  @Override
  public boolean add(E e) {
    addPositioned(e);
    return true;
  }

  /** {@inheritDoc} */
  public long size() {
    return size;
  }

  /** {@inheritDoc} */
  public void clear() {
    try {
      this.size = 0;
      memory.clear();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * Returns an iterator over the elements of this log.
   * 
   * @return an iterator over the elements
   */
  @Override
  public AppendOnlyLogIterator iterator() {
    return new AppendOnlyLogIterator(size);
  }

  @Override
  public void close() throws Exception {
    try {
      memory.header().putLong(0, size);
      memory.close();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  /**
   * An iterator over the elements in this log.
   */
  public class AppendOnlyLogIterator implements Iterator<E> {

    private final long size;
    private long index;
    private long position;

    private AppendOnlyLogIterator(long size) {
      this.size = size;
      index = 0;
      position = 0;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public E next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      long segmentIndex = position / segmentSize;
      long segmentOffset = position % segmentSize;

      ByteBuffer segment = memory.segment((int) segmentIndex);

      int valueSize;
      try {
        valueSize = dataType.size(segment, (int) segmentOffset);
      } catch (IndexOutOfBoundsException e) {
        valueSize = 0;
      }

      if (segmentOffset + valueSize > segmentSize || valueSize == 0) {
        segmentIndex = segmentIndex + 1;
        segmentOffset = 0;
        position = segmentIndex * segmentSize;
        segment = memory.segment((int) segmentIndex);
        valueSize = dataType.size(segment, (int) segmentOffset);
      }
      position += valueSize;
      index++;

      return dataType.read(segment, (int) segmentOffset);
    }

    /**
     * Returns the current position in memory.
     *
     * @return the current position
     */
    public long getPosition() {
      return position;
    }
  }
}
