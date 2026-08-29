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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A list of fixed-size values whose size is a power of two. Such values never straddle a segment
 * and the position of an index is a shift rather than a multiplication, which matters when the list
 * is used as an index that is probed billions of times.
 *
 * <p>
 * The size is persisted in the memory header by {@link #close()}. Appends may be concurrent.
 */
public class MemoryAlignedDataList<E> implements DataList<E> {

  private final FixedSizeDataType<E> dataType;

  private final Memory<?> memory;

  private final int valueShift;

  private final AtomicLong size;

  public MemoryAlignedDataList(FixedSizeDataType<E> dataType) {
    this(dataType, new OffHeapMemory());
  }

  public MemoryAlignedDataList(FixedSizeDataType<E> dataType, Memory<?> memory) {
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
    this.size = new AtomicLong(memory.header().getLong(0));
  }

  @Override
  public long addIndexed(E value) {
    long index = size.getAndIncrement();
    memory.write(dataType, index << valueShift, value);
    return index;
  }

  @Override
  public void set(long index, E value) {
    checkIndex(index);
    memory.write(dataType, index << valueShift, value);
  }

  @Override
  public E get(long index) {
    checkIndex(index);
    return memory.read(dataType, index << valueShift);
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
    try {
      memory.header().putLong(0, size.get());
      memory.close();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }
}
