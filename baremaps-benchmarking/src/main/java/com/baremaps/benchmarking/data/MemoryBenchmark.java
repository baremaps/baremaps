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

package com.baremaps.benchmarking.data;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.LongDataType;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/** The cost of one positioned read or write per memory backing. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class MemoryBenchmark {

  private static final int SEGMENT = 1 << 20;

  private static final int COUNT = 1 << 22;

  private static final int MASK = COUNT - 1;

  @Param({"offheap", "mmapfile", "mmapdir"})
  public String backing;

  private final LongDataType type = new LongDataType();

  private Memory memory;

  private long[] randomPositions;

  private int index;

  @Setup
  public void setup() throws IOException {
    memory = switch (backing) {
      case "offheap" -> Memory.offHeap(SEGMENT);
      case "mmapfile" -> Memory.mappedFile(Files.createTempFile("bench_", ".tmp"), SEGMENT);
      case "mmapdir" -> Memory.mappedDirectory(Files.createTempDirectory("bench_"), SEGMENT);
      default -> throw new IllegalArgumentException(backing);
    };
    for (long i = 0; i < COUNT; i++) {
      memory.write(type, i << 3, i);
    }
    var random = new Random(0);
    randomPositions = new long[COUNT];
    for (int i = 0; i < COUNT; i++) {
      randomPositions[i] = (long) random.nextInt(COUNT) << 3;
    }
  }

  @TearDown
  public void tearDown() throws IOException {
    memory.clear();
    memory.close();
  }

  @Benchmark
  public void writeSequential() {
    long i = index++ & MASK;
    memory.write(type, i << 3, i);
  }

  @Benchmark
  public long readSequential() {
    long i = index++ & MASK;
    return memory.read(type, i << 3);
  }

  @Benchmark
  public long readRandom() {
    return memory.read(type, randomPositions[index++ & MASK]);
  }
}
