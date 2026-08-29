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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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

/**
 * {@link ByteBuffer} against {@link MemorySegment} for the single positioned long access that every
 * collection boils down to. Answers whether moving {@code Memory} to the FFM API would cost or gain
 * anything on the hot path.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class MemorySegmentBenchmark {

  private static final int COUNT = 1 << 22;

  private static final int MASK = COUNT - 1;

  private static final long BYTES = (long) COUNT * Long.BYTES;

  private static final ValueLayout.OfLong LONG = ValueLayout.JAVA_LONG_UNALIGNED;

  @Param({"heap", "direct", "mmap"})
  public String backing;

  private ByteBuffer buffer;

  private MemorySegment segment;

  private Arena arena;

  private long[] randomOffsets;

  private int index;

  @Setup
  public void setup() throws IOException {
    arena = Arena.ofShared();
    switch (backing) {
      case "heap" -> {
        buffer = ByteBuffer.allocate((int) BYTES);
        segment = MemorySegment.ofArray(new byte[(int) BYTES]);
      }
      case "direct" -> {
        buffer = ByteBuffer.allocateDirect((int) BYTES);
        segment = arena.allocate(BYTES, 8);
      }
      case "mmap" -> {
        var file = Files.createTempFile("bench_", ".tmp");
        try (var channel = FileChannel.open(file, StandardOpenOption.READ,
            StandardOpenOption.WRITE)) {
          buffer = channel.map(MapMode.READ_WRITE, 0, BYTES);
          segment = channel.map(MapMode.READ_WRITE, BYTES, BYTES, arena);
        }
      }
      default -> throw new IllegalArgumentException(backing);
    }
    for (int i = 0; i < COUNT; i++) {
      buffer.putLong(i << 3, i);
      segment.set(LONG, (long) i << 3, i);
    }
    var random = new Random(0);
    randomOffsets = new long[COUNT];
    for (int i = 0; i < COUNT; i++) {
      randomOffsets[i] = (long) random.nextInt(COUNT) << 3;
    }
  }

  @TearDown
  public void tearDown() {
    if (buffer instanceof MappedByteBuffer mapped) {
      com.baremaps.data.util.MappedByteBufferUtils.unmap(mapped);
    }
    arena.close();
  }

  @Benchmark
  public long bufferReadSequential() {
    return buffer.getLong((index++ & MASK) << 3);
  }

  @Benchmark
  public long segmentReadSequential() {
    return segment.get(LONG, (long) (index++ & MASK) << 3);
  }

  @Benchmark
  public long bufferReadRandom() {
    return buffer.getLong((int) randomOffsets[index++ & MASK]);
  }

  @Benchmark
  public long segmentReadRandom() {
    return segment.get(LONG, randomOffsets[index++ & MASK]);
  }

  @Benchmark
  public void bufferWriteSequential() {
    int i = index++ & MASK;
    buffer.putLong(i << 3, i);
  }

  @Benchmark
  public void segmentWriteSequential() {
    long i = index++ & MASK;
    segment.set(LONG, i << 3, i);
  }
}
