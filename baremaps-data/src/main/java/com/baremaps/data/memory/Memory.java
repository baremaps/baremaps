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

package com.baremaps.data.memory;

import com.baremaps.data.type.DataType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A growable address space made of fixed-size segments, backed by heap, off-heap, or memory-mapped
 * buffers.
 *
 * <p>
 * Collections address memory with a {@code long} position. A segment size that is a power of two
 * turns the position into a (segment, offset) pair with a shift and a mask; this class owns that
 * arithmetic so that collections never do it themselves. A value must fit inside a single segment,
 * which is what allows a segment to be a plain {@link ByteBuffer}.
 *
 * <p>
 * Segments are allocated lazily, on first access, and are zero-filled by every backing store. Some
 * collections rely on that zero fill to recognize space that has never been written.
 *
 * <p>
 * A separate, small header segment is available for collections to persist their own metadata
 * (sizes, offsets, schemas) alongside the data.
 *
 * <p>
 * Thread safety: segment lookup is lock-free, allocation is synchronized. Concurrent access to the
 * bytes of a segment is the responsibility of the collection.
 */
public abstract class Memory<T extends ByteBuffer> implements AutoCloseable {

  private final int headerSize;

  private final int segmentSize;

  private final int segmentShift;

  private final long segmentMask;

  // Lazily allocated; guarded by "this".
  private T header;

  // Copy-on-grow so that readers can look segments up without locking; guarded by "this" for
  // writes.
  @SuppressWarnings("unchecked")
  private volatile T[] segments = (T[]) new ByteBuffer[0];

  private volatile boolean closed = false;

  /**
   * @param headerSize the size of the header in bytes
   * @param segmentSize the size of the segments in bytes, a power of two
   */
  protected Memory(int headerSize, int segmentSize) {
    if (segmentSize <= 0 || (segmentSize & -segmentSize) != segmentSize) {
      throw new IllegalArgumentException("The segment size must be a power of 2");
    }
    this.headerSize = headerSize;
    this.segmentSize = segmentSize;
    this.segmentShift = Integer.numberOfTrailingZeros(segmentSize);
    this.segmentMask = segmentSize - 1L;
  }

  /** Returns the size of the header in bytes. */
  public int headerSize() {
    return headerSize;
  }

  /** Returns the size of a segment in bytes. */
  public int segmentSize() {
    return segmentSize;
  }

  /** Returns the number of segments allocated so far. */
  public int segmentCount() {
    return segments.length;
  }

  /** Returns the total number of bytes allocated in segments. */
  public long size() {
    return (long) segments.length * segmentSize;
  }

  /** Returns the header, allocating it on first access. */
  public synchronized ByteBuffer header() {
    checkNotClosed();
    if (header == null) {
      header = allocateHeader();
    }
    return header;
  }

  /** Returns the segment at the given index, allocating it and its predecessors if needed. */
  public ByteBuffer segment(int index) {
    T[] current = segments;
    if (index < current.length) {
      return current[index];
    }
    return allocate(index);
  }

  private synchronized T allocate(int index) {
    checkNotClosed();
    if (index >= segments.length) {
      T[] grown = Arrays.copyOf(segments, index + 1);
      try {
        for (int i = segments.length; i <= index; i++) {
          grown[i] = allocateSegment(i);
        }
      } catch (OutOfMemoryError e) {
        throw new MemoryException(
            "Failed to allocate memory segment of size " + segmentSize + " bytes", e);
      } catch (RuntimeException e) {
        throw new MemoryException("Failed to allocate memory segment", e);
      }
      segments = grown;
    }
    return segments[index];
  }

  /**
   * Reads the value stored at the given position.
   *
   * @throws IndexOutOfBoundsException if the position is negative or beyond the addressable range
   */
  public <E> E read(DataType<E> dataType, long position) {
    int index = segmentIndex(position);
    int offset = (int) (position & segmentMask);
    return dataType.read(segment(index), offset);
  }

  /**
   * Writes a value at the given position.
   *
   * @throws IndexOutOfBoundsException if the position is negative, beyond the addressable range, or
   *         if the value would cross a segment boundary
   */
  public <E> void write(DataType<E> dataType, long position, E value) {
    int index = segmentIndex(position);
    int offset = (int) (position & segmentMask);
    int size = dataType.size(value);
    if (offset + size > segmentSize) {
      throw new IndexOutOfBoundsException(
          "Value of " + size + " bytes at position " + position + " crosses a segment boundary");
    }
    dataType.write(segment(index), offset, value);
  }

  /**
   * Returns the size of the value encoded at the given position, or 0 if no value can start there
   * because the rest of the segment is too small to hold one or has never been written.
   */
  public int sizeOf(DataType<?> dataType, long position) {
    int index = segmentIndex(position);
    int offset = (int) (position & segmentMask);
    int size;
    try {
      size = dataType.size(segment(index), offset);
    } catch (IndexOutOfBoundsException e) {
      return 0;
    }
    return size > segmentSize - offset ? 0 : size;
  }

  /** Returns the number of bytes available in the segment holding the given position. */
  public int remaining(long position) {
    return segmentSize - (int) (position & segmentMask);
  }

  private int segmentIndex(long position) {
    long index = position >>> segmentShift;
    if (position < 0 || index > Integer.MAX_VALUE) {
      throw new IndexOutOfBoundsException("Position out of range: " + position);
    }
    return (int) index;
  }

  /** Returns whether the memory has been closed. */
  public boolean isClosed() {
    return closed;
  }

  protected void checkNotClosed() {
    if (closed) {
      throw new IllegalStateException("Memory has been closed");
    }
  }

  /** Allocates the header buffer. */
  protected abstract T allocateHeader();

  /** Allocates the segment at the given index. Must return a zero-filled buffer. */
  protected abstract T allocateSegment(int index);

  /** Releases a buffer allocated by this memory; the default does nothing. */
  protected void release(T buffer) throws IOException {}

  /** Deletes the underlying storage, if any; the default does nothing. */
  protected void delete() throws IOException {}

  /**
   * Releases the buffers. The memory can no longer be used afterwards, but {@link #clear()} may
   * still be called to delete the storage.
   */
  @Override
  public final synchronized void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    releaseBuffers();
  }

  /**
   * Releases the buffers and deletes the storage. Unless the memory is closed, it can be used again
   * and grows from scratch.
   */
  @SuppressWarnings("unchecked")
  public final synchronized void clear() throws IOException {
    releaseBuffers();
    header = null;
    segments = (T[]) new ByteBuffer[0];
    delete();
  }

  private void releaseBuffers() throws IOException {
    if (header != null) {
      release(header);
    }
    for (T segment : segments) {
      release(segment);
    }
  }
}
