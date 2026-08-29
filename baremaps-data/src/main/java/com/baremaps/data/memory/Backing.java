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
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Where the bytes of a {@link Memory} come from. The arena owns their lifetime; a backing only
 * produces them and deletes their storage.
 */
sealed interface Backing {

  MemorySegment header(Arena arena, long size);

  /** Returns the segment at the given index: zero-filled, or its existing bytes when reopened. */
  MemorySegment segment(Arena arena, int index, long size);

  /** Deletes the storage, if any. Called once the arena is closed, so nothing is mapped. */
  default void delete() throws IOException {}

  /** Native memory, freed when the arena closes. */
  record Native() implements Backing {

    @Override
    public MemorySegment header(Arena arena, long size) {
      return arena.allocate(size);
    }

    @Override
    public MemorySegment segment(Arena arena, int index, long size) {
      return arena.allocate(size);
    }
  }

  /** One file: the header first, then the segments back to back. Mapping past its end grows it. */
  record MappedFile(Path file) implements Backing {

    @Override
    public MemorySegment header(Arena arena, long size) {
      return map(arena, file, 0, size);
    }

    @Override
    public MemorySegment segment(Arena arena, int index, long size) {
      return map(arena, file, Memory.HEADER_BYTES + index * size, size);
    }

    @Override
    public void delete() throws IOException {
      Files.deleteIfExists(file);
    }
  }

  /**
   * One file per segment, plus a header file. Small mappings, and a data set that is not bound by
   * what one file can hold.
   */
  record MappedDirectory(Path directory) implements Backing {

    @Override
    public MemorySegment header(Arena arena, long size) {
      return map(arena, directory.resolve("header"), 0, size);
    }

    @Override
    public MemorySegment segment(Arena arena, int index, long size) {
      return map(arena, directory.resolve(index + ".part"), 0, size);
    }

    @Override
    public void delete() throws IOException {
      if (Files.exists(directory)) {
        FileUtils.deleteRecursively(directory);
      }
    }
  }

  private static MemorySegment map(Arena arena, Path file, long position, long size) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      // The mapping outlives the channel: the arena, not the channel, decides when it is unmapped.
      try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
          StandardOpenOption.READ, StandardOpenOption.WRITE)) {
        return channel.map(MapMode.READ_WRITE, position, size, arena);
      }
    } catch (IOException e) {
      throw new MemoryException(e);
    }
  }
}
