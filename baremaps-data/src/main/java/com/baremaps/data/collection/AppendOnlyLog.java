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

/**
 * A log of values of variable size, appended back to back and addressed by their position.
 *
 * <p>
 * A value never crosses a segment boundary: when it does not fit in the remaining bytes of a
 * segment, it starts the next one. The skipped tail stays zero-filled, which is how the iterator
 * recognizes it (see {@link DataType#size(ByteBuffer, int)}).
 *
 * <p>
 * The size and the end position are persisted in the first {@link #HEADER_BYTES} bytes of the
 * memory header by {@link #flush()} and {@link #close()}, so that a log stored in a memory-mapped
 * file can be reopened and appended to. Other users of the same header must write after them.
 *
 * <p>
 * Thread safety: appends may be concurrent, the reservation of a position is synchronized and the
 * write itself is not. Reads of a position returned by {@link #addPositioned(Object)} are safe once
 * that call has returned.
 */
public class AppendOnlyLog<E> implements DataCollection<E> {

  /** The number of header bytes reserved by the log. */
  public static final int HEADER_BYTES = 2 * Long.BYTES;

  private static final int SIZE_OFFSET = 0;

  private static final int END_OFFSET = Long.BYTES;

  private final DataType<E> dataType;

  private final Memory<?> memory;

  // The number of values and the position of the next write, guarded by "this".
  private long size;

  private long end;

  /** Creates a log in off-heap memory. */
  public AppendOnlyLog(DataType<E> dataType) {
    this(dataType, new OffHeapMemory());
  }

  /** Creates a log in the given memory, reopening it if the memory holds a header. */
  public AppendOnlyLog(DataType<E> dataType, Memory<?> memory) {
    this.dataType = dataType;
    this.memory = memory;
    ByteBuffer header = memory.header();
    this.size = header.getLong(SIZE_OFFSET);
    this.end = header.getLong(END_OFFSET);
  }

  /**
   * Appends a value and returns its position.
   *
   * @throws DataCollectionException if the value is larger than a segment
   */
  public long addPositioned(E value) {
    int valueSize = dataType.size(value);
    if (valueSize > memory.segmentSize()) {
      throw new DataCollectionException("The value is too big to fit in a segment");
    }
    long position;
    synchronized (this) {
      position = end;
      if (memory.remaining(position) < valueSize) {
        position += memory.remaining(position);
      }
      end = position + valueSize;
      size++;
    }
    memory.write(dataType, position, value);
    return position;
  }

  /** Returns the value at a position returned by {@link #addPositioned(Object)}. */
  public E getPositioned(long position) {
    return memory.read(dataType, position);
  }

  @Override
  public boolean add(E e) {
    addPositioned(e);
    return true;
  }

  @Override
  public synchronized long size() {
    return size;
  }

  /** Persists the size and end position in the memory header. */
  public synchronized void flush() {
    ByteBuffer header = memory.header();
    header.putLong(SIZE_OFFSET, size);
    header.putLong(END_OFFSET, end);
  }

  @Override
  public synchronized void clear() {
    try {
      size = 0;
      end = 0;
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
      flush();
      memory.close();
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<>() {
      private final long expected = size();
      private long index = 0;
      private long position = 0;

      @Override
      public boolean hasNext() {
        return index < expected;
      }

      @Override
      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        int valueSize = memory.sizeOf(dataType, position);
        if (valueSize == 0) {
          // Nothing fits in the tail of this segment: the value starts the next one.
          position += memory.remaining(position);
          valueSize = memory.sizeOf(dataType, position);
          if (valueSize == 0) {
            throw new DataCollectionException("Corrupted log at position " + position);
          }
        }
        E value = memory.read(dataType, position);
        position += valueSize;
        index++;
        return value;
      }
    };
  }
}
