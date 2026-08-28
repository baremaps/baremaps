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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** A base class for managing on-heap, off-heap, or memory-mapped segments. */
public abstract class Memory<T extends ByteBuffer> implements AutoCloseable {

  private final int headerSize;

  private final int segmentSize;

  private final long segmentShift;

  private final long segmentMask;

  protected T header;

  protected List<T> segments = new ArrayList<>();

  // Flag to track if this Memory has been closed
  protected volatile boolean closed = false;

  /**
   * Constructs a memory with the specified segment size.
   *
   * @param segmentSize the size of the segments (must be a power of 2)
   * @throws IllegalArgumentException if the segment size is not a power of 2
   */
  protected Memory(int headerSize, int segmentSize) {
    if ((segmentSize & -segmentSize) != segmentSize) {
      throw new IllegalArgumentException("The segment size must be a power of 2");
    }
    this.headerSize = headerSize;
    this.segmentSize = segmentSize;
    this.segmentShift = (long) (Math.log(this.segmentSize) / Math.log(2));
    this.segmentMask = this.segmentSize - 1l;
  }

  /**
   * Returns the size of the header.
   *
   * @return the size of the header
   */
  public int headerSize() {
    return headerSize;
  }

  /**
   * Returns the size of the segments. >>>>>>> 251ea904 (Add header to memory)
   *
   * @return the segment size in bytes
   */
  public int segmentSize() {
    return segmentSize;
  }

  /**
   * Returns the bit shift used to find a segment index from a memory position.
   *
   * @return the bit shift value
   */
  public long segmentShift() {
    return segmentShift;
  }

  /**
   * Returns the bit mask used to find an offset within a segment from a memory position.
   *
   * @return the bit mask value
   */
  public long segmentMask() {
    return segmentMask;
  }

  public ByteBuffer header() {
    if (header == null) {
      synchronized (this) {
        header = allocateHeader();
      }
    }
    return header;
  }

  /**
   * Allocates a header.
   *
   * @return the header
   */
  protected abstract T allocateHeader();

  /**
   * Returns a segment at the specified index, allocating it if necessary.
   *
   * @param index the segment index
   * @return the segment as a ByteBuffer
   * @throws MemoryException if segment allocation fails
   */
  public ByteBuffer segment(int index) {
    if (segments.size() <= index) {
      return allocateSegmentInternal(index);
    }
    ByteBuffer segment = segments.get(index);
    if (segment == null) {
      return allocateSegmentInternal(index);
    }
    return segment;
  }

  /**
   * Checks if this Memory has been closed.
   * 
   * @throws IllegalStateException if this Memory has been closed
   */
  protected void checkNotClosed() {
    if (closed) {
      throw new IllegalStateException("Memory has been closed");
    }
  }

  /**
   * Allocates a segment at the specified index. Thread-safe method.
   *
   * @param index the segment index
   * @return the allocated segment
   * @throws MemoryException if allocation fails
   */
  private synchronized ByteBuffer allocate(int index) {
    checkNotClosed();

    try {
      while (segments.size() <= index) {
        segments.add(null);
      }
      T segment = segments.get(index);
      if (segment == null) {
        segment = allocateSegment(index);
        segments.set(index, segment);
      }
      return segment;
    } catch (OutOfMemoryError e) {
      throw new MemoryException(
          "Failed to allocate memory segment of size " + segmentSize + " bytes", e);
    } catch (Exception e) {
      throw new MemoryException("Failed to allocate memory segment", e);
    }
  }

  /**
   * Returns the total size of allocated memory.
   *
   * @return the size in bytes
   */
  public long size() {
    checkNotClosed();
    return (long) segments.size() * (long) segmentSize;
  }

  /** The allocation of segments is synchronized to enable access by multiple threads. */
  private synchronized ByteBuffer allocateSegmentInternal(int index) {
    while (segments.size() <= index) {
      segments.add(null);
    }
    T segment = segments.get(index);
    if (segment == null) {
      segment = allocateSegment(index);
      segments.set(index, segment);
    }
    return segment;
  }

  /**
   * Allocates a segment of the specified size.
   *
   * @param index the index of the segment
   * @return the segment
   */
  protected abstract T allocateSegment(int index);

  /**
   * Releases resources associated with this memory. Unlike {@link #clear()}, this method does not
   * delete underlying data.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override
  public synchronized void close() throws IOException {
    if (!closed) {
      closed = true;
    }
  }

  /**
   * Deletes all data managed by this memory. Unlike {@link #close()}, this method removes
   * underlying data.
   *
   * @throws IOException if an I/O error occurs
   */
  public abstract void clear() throws IOException;

}
