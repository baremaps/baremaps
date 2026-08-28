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


/** Memory implementation that uses memory-mapped files in a directory for storage. */
public class MemoryMappedDirectory extends Memory<MappedByteBuffer> {

  private final Path directory;

  /**
   * Constructs a MemoryMappedDirectory with the specified directory and 1GB segment size.
   *
   * @param directory the directory to store segments in
   */
  public MemoryMappedDirectory(Path directory) {
    this(directory, 1 << 30);
    if (!Files.exists(directory)) {
      try {
        Files.createDirectories(directory);
      } catch (IOException e) {
        throw new MemoryException(e);
      }
    }
  }

  /**
   * Constructs a MemoryMappedDirectory with the specified directory and segment size.
   *
   * @param directory the directory to store segments in
   * @param segmentSize the size of each segment in bytes
   */
  public MemoryMappedDirectory(Path directory, int segmentSize) {
    super(1 << 14, segmentSize);
    this.directory = directory;
  }

  @Override
  protected MappedByteBuffer allocateHeader() {
    try {
      Path file = directory.resolve("header");
      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        return channel.map(MapMode.READ_WRITE, 0, headerSize());
      }
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected MappedByteBuffer allocateSegment(int index) {
    try {
      Path file = directory.resolve(String.format("%s.part", index));
      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        return channel.map(MapMode.READ_WRITE, 0, segmentSize());
      }
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }

  /**
   * Releases all mapped buffers but keeps the files intact.
   * 
   * {@inheritDoc}
   */
  @Override
  public synchronized void close() throws IOException {
    MappedByteBufferUtils.unmap(header);
    for (MappedByteBuffer buffer : segments) {
      if (buffer != null) {
        MappedByteBufferUtils.unmap(buffer);
      }
    }
  }

  /**
   * Unmaps all buffers and deletes the directory with all its files.
   * 
   * {@inheritDoc}
   */
  @Override
  public synchronized void clear() throws IOException {
    // Release resources first
    close();

    // Clear the header and segment list
    header.clear();
    segments.clear();

    // Delete the directory and all files in it if it exists
    if (Files.exists(directory)) {
      FileUtils.deleteRecursively(directory);
    }
  }
}
