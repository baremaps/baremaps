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

/** Memory implementation that uses off-heap (direct) byte buffers for storage. */
public class OffHeapMemory extends Memory<ByteBuffer> {

  /** Constructs an OffHeapMemory with a 1MB default segment size. */
  public OffHeapMemory() {
    this(1 << 20);
  }

  /**
   * Constructs an OffHeapMemory with the specified segment size.
   *
   * @param segmentSize the size of each segment in bytes
   */
  public OffHeapMemory(int segmentSize) {
    super(1024, segmentSize);
  }

  /** {@inheritDoc} */
  @Override
  protected ByteBuffer allocateHeader() {
    return ByteBuffer.allocateDirect(headerSize());
  }

  /** {@inheritDoc} */
  @Override
  protected ByteBuffer allocateSegment(int index) {
    return ByteBuffer.allocateDirect(segmentSize());
  }

  /**
   * Direct buffers are managed by GC and can't be explicitly freed in Java 8.
   * 
   * {@inheritDoc}
   */
  @Override
  public synchronized void close() throws IOException {
    // Direct ByteBuffers are managed by GC - we can't explicitly free them in Java 8
    // In Java 9+, we could use Cleaner or Unsafe API if needed
  }

  /**
   * Clears all segments to allow garbage collection.
   * 
   * {@inheritDoc}
   */
  @Override
  public synchronized void clear() throws IOException {
    // Clear the segment list to allow GC to reclaim the memory
    segments.clear();
  }
}
