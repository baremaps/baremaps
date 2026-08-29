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

import com.baremaps.data.util.FileUtils;
import com.baremaps.data.util.MappedByteBufferUtils;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A memory backed by a directory holding one file per segment ({@code 0.part}, {@code 1.part}, ...)
 * and a {@code header} file. One file per segment keeps each mapping small and lets a data set span
 * more than a single file system can address in one file.
 */
public class MemoryMappedDirectory extends Memory<MappedByteBuffer> {

  private final Path directory;

  public MemoryMappedDirectory(Path directory) {
    this(directory, 1 << 30);
  }

  public MemoryMappedDirectory(Path directory, int segmentSize) {
    super(1 << 14, segmentSize);
    this.directory = directory;
  }

  @Override
  protected MappedByteBuffer allocateHeader() {
    return map(directory.resolve("header"), headerSize());
  }

  @Override
  protected MappedByteBuffer allocateSegment(int index) {
    return map(directory.resolve(index + ".part"), segmentSize());
  }

  private MappedByteBuffer map(Path file, int size) {
    try {
      Files.createDirectories(directory);
      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        return channel.map(MapMode.READ_WRITE, 0, size);
      }
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
    if (Files.exists(directory)) {
      FileUtils.deleteRecursively(directory);
    }
  }
}
