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

import java.nio.ByteBuffer;

/**
 * A memory backed by direct buffers, which keeps large data sets out of the garbage collector's
 * way. The buffers are reclaimed by the garbage collector once unreferenced.
 */
public class OffHeapMemory extends Memory<ByteBuffer> {

  public OffHeapMemory() {
    this(1 << 20);
  }

  public OffHeapMemory(int segmentSize) {
    super(1024, segmentSize);
  }

  @Override
  protected ByteBuffer allocateHeader() {
    return ByteBuffer.allocateDirect(headerSize());
  }

  @Override
  protected ByteBuffer allocateSegment(int index) {
    return ByteBuffer.allocateDirect(segmentSize());
  }
}
