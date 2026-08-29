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

import com.baremaps.data.collection.AppendOnlyLog;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.DataType;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.LongListDataType;
import com.baremaps.data.type.StringDataType;
import java.util.List;
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
import org.openjdk.jmh.infra.Blackhole;

/** Appending, positioned reads and iteration for fixed and variable-size values. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class AppendOnlyLogBenchmark {

  private static final int COUNT = 1 << 20;

  private static final int MASK = COUNT - 1;

  @Param({"long", "string", "longlist"})
  public String kind;

  private DataType<Object> type;

  private Object[] values;

  private AppendOnlyLog<Object> filled;

  private AppendOnlyLog<Object> appending;

  private long[] positions;

  private int index;

  @Setup
  @SuppressWarnings("unchecked")
  public void setup() {
    var random = new Random(0);
    values = new Object[COUNT];
    switch (kind) {
      case "long" -> {
        type = (DataType<Object>) (DataType<?>) new LongDataType();
        for (int i = 0; i < COUNT; i++) {
          values[i] = (long) i;
        }
      }
      case "string" -> {
        type = (DataType<Object>) (DataType<?>) new StringDataType();
        for (int i = 0; i < COUNT; i++) {
          values[i] = "value-" + i + "-" + "x".repeat(random.nextInt(32));
        }
      }
      case "longlist" -> {
        type = (DataType<Object>) (DataType<?>) new LongListDataType();
        for (int i = 0; i < COUNT; i++) {
          values[i] = List.of((long) i, (long) i + 1, (long) i + 2, (long) i + 3);
        }
      }
      default -> throw new IllegalArgumentException(kind);
    }
    filled = new AppendOnlyLog<>(type, Memory.offHeap());
    positions = new long[COUNT];
    for (int i = 0; i < COUNT; i++) {
      positions[i] = filled.addPositioned(values[i]);
    }
    appending = new AppendOnlyLog<>(type, Memory.offHeap());
  }

  @TearDown
  public void tearDown() {
    filled.close();
    appending.close();
  }

  @Benchmark
  public long append() {
    return appending.addPositioned(values[index++ & MASK]);
  }

  @Benchmark
  public Object getPositioned() {
    return filled.getPositioned(positions[index++ & MASK]);
  }

  /** Iterates the whole log; the time is for all {@code COUNT} values. */
  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public void iterate(Blackhole blackhole) {
    for (Object value : filled) {
      blackhole.consume(value);
    }
  }
}
