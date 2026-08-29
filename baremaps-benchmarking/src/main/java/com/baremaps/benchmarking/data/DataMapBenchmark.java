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

import com.baremaps.data.collection.DataMap;
import com.baremaps.data.collection.DenseDataMap;
import com.baremaps.data.collection.SparseDataMap;
import com.baremaps.data.type.LongDataType;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
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
 * Filling and probing the maps that back an OpenStreetMap import. Keys are increasing with small
 * gaps, as OSM ids are.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class DataMapBenchmark {

  private static final int COUNT = 1 << 22;

  private static final int MASK = COUNT - 1;

  private static long key(int i) {
    return i * 3L;
  }

  private static DataMap<Long, Long> create(String kind) {
    return switch (kind) {
      case "monotonic" -> new SparseDataMap<>(new LongDataType());
      case "paged" -> new DenseDataMap<>(new LongDataType());
      default -> throw new IllegalArgumentException(kind);
    };
  }

  @State(Scope.Benchmark)
  public static class Filled {

    @Param({"monotonic", "paged"})
    public String kind;

    DataMap<Long, Long> map;

    long[] randomKeys;

    int index;

    @Setup
    public void setup() {
      map = create(kind);
      for (int i = 0; i < COUNT; i++) {
        map.put(key(i), (long) i);
      }
      var random = new Random(0);
      randomKeys = new long[COUNT];
      for (int i = 0; i < COUNT; i++) {
        randomKeys[i] = key(random.nextInt(COUNT));
      }
    }

    @TearDown
    public void tearDown() throws Exception {
      map.close();
    }
  }

  @State(Scope.Benchmark)
  public static class Empty {

    @Param({"monotonic", "paged"})
    public String kind;

    DataMap<Long, Long> map;

    @Setup(Level.Invocation)
    public void setup() {
      map = create(kind);
    }

    @TearDown(Level.Invocation)
    public void tearDown() throws Exception {
      map.close();
    }
  }

  @Benchmark
  public Long getSequential(Filled state) {
    return state.map.get(key(state.index++ & MASK));
  }

  @Benchmark
  public Long getRandom(Filled state) {
    return state.map.get(state.randomKeys[state.index++ & MASK]);
  }

  /** Fills a fresh map; the time is for all {@code COUNT} puts. */
  @Benchmark
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  @Warmup(iterations = 2)
  @Measurement(iterations = 3)
  public DataMap<Long, Long> putAll(Empty state) {
    for (int i = 0; i < COUNT; i++) {
      state.map.put(key(i), (long) i);
    }
    return state.map;
  }
}
