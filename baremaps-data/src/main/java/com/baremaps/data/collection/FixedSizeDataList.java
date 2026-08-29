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

import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A list of fixed-size values packed back to back. Values may straddle segments as far as the
 * arithmetic goes, so the segment size must be a multiple of the value size; see
 * {@link MemoryAlignedDataList} for the faster power-of-two variant.
 *
 * <p>
 * The size is persisted in the memory header by {@link #close()}. Appends may be concurrent.
 */
public class FixedSizeDataList<E> implements DataList<E> {

  private final FixedSizeDataType<E> dataType;

  private final Memory memory;

  private final AtomicLong size;

  public FixedSizeDataList(FixedSizeDataType<E> dataType) {
    this(dataType, Memory.offHeap());
  }

  public FixedSizeDataList(FixedSizeDataType<E> dataType, Memory memory) {
    if (dataType.size() > memory.segmentSize()) {
      throw new DataCollectionException("The segment size is too small for the data type");
    }
    if (memory.segmentSize() % dataType.size() != 0) {
      throw new DataCollectionException("The segment size must be a multiple of the value size");
    }
    this.dataType = dataType;
    this.memory = memory;
    this.size = new AtomicLong(memory.header().get(JAVA_LONG_UNALIGNED, 0));
  }

  @Override
  public long addIndexed(E value) {
    long index = size.getAndIncrement();
    memory.write(dataType, index * dataType.size(), value);
    return index;
  }

  @Override
  public void set(long index, E value) {
    checkIndex(index);
    memory.write(dataType, index * dataType.size(), value);
  }

  @Override
  public E get(long index) {
    checkIndex(index);
    return memory.read(dataType, index * dataType.size());
  }

  private void checkIndex(long index) {
    if (index < 0 || index >= size.get()) {
      throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
    }
  }

  @Override
  public long size() {
    return size.get();
  }

  @Override
  public void clear() {
    try {
      size.set(0);
      memory.clear();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public void close() {
    if (memory.isClosed()) {
      return;
    }
    memory.header().set(JAVA_LONG_UNALIGNED, 0, size.get());
    memory.close();
  }
}
