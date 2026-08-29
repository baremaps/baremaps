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

import com.baremaps.data.util.MappedByteBufferUtils;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A memory backed by a single file: the header first, then the segments back to back. The file
 * grows as segments are mapped.
 */
public class MemoryMappedFile extends Memory<MappedByteBuffer> {

  private final Path file;

  public MemoryMappedFile(Path file) {
    this(file, 1 << 30);
  }

  public MemoryMappedFile(Path file, int segmentSize) {
    super(1024, segmentSize);
    this.file = file;
  }

  @Override
  protected MappedByteBuffer allocateHeader() {
    return map(0, headerSize());
  }

  @Override
  protected MappedByteBuffer allocateSegment(int index) {
    return map(headerSize() + (long) index * segmentSize(), segmentSize());
  }

  private MappedByteBuffer map(long position, int size) {
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
        StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      return channel.map(MapMode.READ_WRITE, position, size);
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }

  @Override
  protected void release(MappedByteBuffer buffer) {
    MappedByteBufferUtils.unmap(buffer);
  }

  @Override
  protected void delete() throws IOException {
    Files.deleteIfExists(file);
  }
}
