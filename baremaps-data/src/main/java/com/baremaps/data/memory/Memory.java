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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * A growable address space made of fixed-size segments, in native memory or in memory-mapped files.
 *
 * <p>
 * Collections address memory with a {@code long} position. A segment size that is a power of two
 * turns the position into a (segment, offset) pair with a shift and a mask; this class owns that
 * arithmetic so that collections never do it themselves. A value must fit inside a single segment.
 *
 * <p>
 * Segments are allocated lazily, on first access, and are zero-filled by every backing. Some
 * collections rely on that zero fill to recognize space that has never been written. A separate
 * header segment of {@link #HEADER_BYTES} lets collections persist their own metadata (sizes,
 * offsets, schemas) alongside the data.
 *
 * <p>
 * All segments belong to one {@link Arena}: {@link #close()} frees or unmaps them at once, and any
 * later access fails with an {@link IllegalStateException}. Memory that is never closed is never
 * reclaimed, so callers own the lifetime.
 *
 * <p>
 * Thread safety: segment lookup is lock-free, allocation is synchronized, and the arena is shared,
 * so any thread may read or write. Concurrent access to the bytes of a segment is the
 * responsibility of the collection.
 */
public final class Memory implements AutoCloseable {

  /** The size of the header, large enough for a serialized table schema. */
  public static final int HEADER_BYTES = 1 << 14;

  private final Backing backing;

  private final long segmentSize;

  private final int segmentShift;

  private final long segmentMask;

  // Replaced together by clear(); volatile for lock-free readers, guarded by "this" for writers.
  private volatile Arena arena;

  private volatile MemorySegment header;

  private volatile MemorySegment[] segments;

  /** Creates a memory in native memory with 1 MB segments. */
  public static Memory offHeap() {
    return offHeap(1 << 20);
  }

  /** Creates a memory in native memory. */
  public static Memory offHeap(long segmentSize) {
    return new Memory(new Backing.Native(), segmentSize);
  }

  /** Creates or reopens a memory in a single file with 1 GB segments. */
  public static Memory mappedFile(Path file) {
    return mappedFile(file, 1L << 30);
  }

  /** Creates or reopens a memory in a single file. */
  public static Memory mappedFile(Path file, long segmentSize) {
    return new Memory(new Backing.MappedFile(file), segmentSize);
  }

  /** Creates or reopens a memory in a directory with 1 GB segments, one file each. */
  public static Memory mappedDirectory(Path directory) {
    return mappedDirectory(directory, 1L << 30);
  }

  /** Creates or reopens a memory in a directory, one file per segment. */
  public static Memory mappedDirectory(Path directory, long segmentSize) {
    return new Memory(new Backing.MappedDirectory(directory), segmentSize);
  }

  private Memory(Backing backing, long segmentSize) {
    if (segmentSize <= 0 || (segmentSize & -segmentSize) != segmentSize) {
      throw new IllegalArgumentException("The segment size must be a power of 2");
    }
    this.backing = backing;
    this.segmentSize = segmentSize;
    this.segmentShift = Long.numberOfTrailingZeros(segmentSize);
    this.segmentMask = segmentSize - 1;
    open();
  }

  private void open() {
    arena = Arena.ofShared();
    header = backing.header(arena, HEADER_BYTES);
    segments = new MemorySegment[0];
  }

  /** Returns the size of a segment in bytes. */
  public long segmentSize() {
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

  /** Returns the header. */
  public MemorySegment header() {
    return header;
  }

  /** Returns the segment at the given index, allocating it and its predecessors if needed. */
  MemorySegment segment(int index) {
    MemorySegment[] current = segments;
    if (index < current.length) {
      return current[index];
    }
    return allocate(index);
  }

  private synchronized MemorySegment allocate(int index) {
    if (index >= segments.length) {
      MemorySegment[] grown = Arrays.copyOf(segments, index + 1);
      try {
        for (int i = segments.length; i <= index; i++) {
          grown[i] = backing.segment(arena, i, segmentSize);
        }
      } catch (OutOfMemoryError e) {
        throw new MemoryException("Failed to allocate a segment of " + segmentSize + " bytes", e);
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
    return dataType.read(segment(segmentIndex(position)), position & segmentMask);
  }

  /**
   * Writes a value at the given position.
   *
   * @throws IndexOutOfBoundsException if the position is negative, beyond the addressable range, or
   *         if the value would cross a segment boundary
   */
  public <E> void write(DataType<E> dataType, long position, E value) {
    long offset = position & segmentMask;
    int size = dataType.size(value);
    if (offset + size > segmentSize) {
      throw new IndexOutOfBoundsException(
          "Value of " + size + " bytes at position " + position + " crosses a segment boundary");
    }
    dataType.write(segment(segmentIndex(position)), offset, value);
  }

  /**
   * Returns the size of the value encoded at the given position, or 0 if no value can start there
   * because the rest of the segment is too small to hold one or has never been written.
   */
  public int sizeOf(DataType<?> dataType, long position) {
    long offset = position & segmentMask;
    int size;
    try {
      size = dataType.size(segment(segmentIndex(position)), offset);
    } catch (IndexOutOfBoundsException e) {
      return 0;
    }
    return size > segmentSize - offset ? 0 : size;
  }

  /** Returns the number of bytes available in the segment holding the given position. */
  public long remaining(long position) {
    return segmentSize - (position & segmentMask);
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
    return !arena.scope().isAlive();
  }

  /** Frees or unmaps every segment; any later access throws. Idempotent. */
  @Override
  public synchronized void close() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  /** Releases the segments, deletes the storage, and starts again from scratch. */
  public synchronized void clear() throws IOException {
    close();
    backing.delete();
    open();
  }
}
