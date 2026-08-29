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
import java.nio.file.Files;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/** Provides one instance of every memory implementation, with small segments. */
public class MemoryProvider {

  public static final int SEGMENT_BYTES = 1 << 10;

  public static Stream<Arguments> memories() throws IOException {
    return Stream.of(
        Arguments.of(Memory.offHeap(SEGMENT_BYTES)),
        Arguments.of(Memory.mappedFile(Files.createTempFile("baremaps_", ".tmp"), SEGMENT_BYTES)),
        Arguments
            .of(Memory.mappedDirectory(Files.createTempDirectory("baremaps_"), SEGMENT_BYTES)));
  }
}
