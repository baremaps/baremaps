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
import com.baremaps.data.collection.FixedSizeDataList;
import com.baremaps.data.collection.SparseDataMap;
import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.LonLatDataType;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.locationtech.jts.geom.Coordinate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * The node index of a planet import, scaled down: node ids are increasing and about 75% dense (the
 * planet has ~9.5 billion nodes below an id of ~12.5 billion), coordinates are the 8-byte
 * {@link LonLatDataType}. The fill is one sequential pass, as the nodes of a PBF file come. The
 * lookups follow the ways: a way reads a run of nodes whose ids were minted together, so they sit
 * close, and consecutive ways drift around the id space.
 *
 * <p>
 * {@code flat} is the lower bound: a flat array without presence, so it cannot tell a missing node
 * from one at (-180, -90). {@code pagedNN} is a {@link DenseDataMap} with pages of 2^NN keys.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class OsmIndexBenchmark {

  /** Ids per benchmark: 2^24 ids, ~12.6 million present nodes, ~100 MB per map. */
  private static final int ID_SPACE = 1 << 24;

  private static final int WAYS = 1 << 16;

  private static final int WAY_MASK = WAYS - 1;

  private static final int NODES_PER_WAY = 16;

  private static final LonLatDataType LONLAT = new LonLatDataType();

  /** The ids that exist, increasing, about 75% of the space. */
  static long[] presentIds() {
    var random = new Random(7);
    var ids = new long[ID_SPACE];
    int n = 0;
    for (long id = 0; id < ID_SPACE; id++) {
      if (random.nextInt(4) != 0) {
        ids[n++] = id;
      }
    }
    return java.util.Arrays.copyOf(ids, n);
  }

  /** For each way, the index in {@code ids} of its first node; the next 15 nodes follow. */
  static int[] wayStarts(int idCount) {
    var random = new Random(11);
    var starts = new int[WAYS];
    int cursor = random.nextInt(idCount);
    for (int i = 0; i < WAYS; i++) {
      // Most ways sit near the previous one; some jump elsewhere.
      if (random.nextInt(8) == 0) {
        cursor = random.nextInt(idCount - NODES_PER_WAY);
      } else {
        cursor = Math.floorMod(cursor + random.nextInt(-2000, 2000), idCount - NODES_PER_WAY);
      }
      starts[i] = cursor;
    }
    return starts;
  }

  static DataMap<Long, Coordinate> create(String kind) {
    return switch (kind) {
      case "flat" -> flat();
      case "monotonic" -> new SparseDataMap<>(new FixedSizeDataList<>(LONLAT));
      case "paged08" -> paged(8);
      case "paged10" -> paged(10);
      case "paged12" -> paged(12);
      case "paged14" -> paged(14);
      default -> throw new IllegalArgumentException(kind);
    };
  }

  /**
   * The lower bound: a flat array addressed by the id with no notion of absence, so a missing node
   * reads as (-180, -90). Not a usable index, only the cost a presence check is measured against.
   */
  private static DataMap<Long, Coordinate> flat() {
    var memory = Memory.offHeap();
    return new DataMap<>() {
      @Override
      public long size() {
        return memory.size() >>> 3;
      }

      @Override
      public Coordinate get(Object key) {
        return memory.read(LONLAT, (Long) key << 3);
      }

      @Override
      public Coordinate put(Long key, Coordinate value) {
        memory.write(LONLAT, key << 3, value);
        return null;
      }

      @Override
      public boolean containsKey(Object key) {
        return true;
      }

      @Override
      public boolean containsValue(Object value) {
        return false;
      }

      @Override
      public void clear() {}

      @Override
      public java.util.Iterator<java.util.Map.Entry<Long, Coordinate>> entryIterator() {
        return java.util.Collections.emptyIterator();
      }

      @Override
      public void close() {
        memory.close();
      }
    };
  }

  private static DenseDataMap<Coordinate> paged(int pageShift) {
    return new DenseDataMap<>(LONLAT, pageShift, Memory.offHeap(), Memory.offHeap(),
        Memory.offHeap());
  }

  @State(Scope.Benchmark)
  public static class Filled {

    @Param({"flat", "monotonic", "paged08", "paged10", "paged12", "paged14"})
    public String kind;

    DataMap<Long, Coordinate> map;

    long[] ids;

    int[] starts;

    int way;

    @Setup
    public void setup() {
      ids = presentIds();
      map = create(kind);
      for (long id : ids) {
        map.put(id, new Coordinate(id % 360 - 180, id % 180 - 90));
      }
      starts = wayStarts(ids.length);
    }

    @TearDown
    public void tearDown() throws Exception {
      map.close();
    }
  }

  @State(Scope.Benchmark)
  public static class Empty {

    @Param({"flat", "monotonic", "paged08", "paged10", "paged12", "paged14"})
    public String kind;

    DataMap<Long, Coordinate> map;

    long[] ids;

    @Setup
    public void setupIds() {
      ids = presentIds();
    }

    @Setup(Level.Invocation)
    public void setup() {
      map = create(kind);
    }

    @TearDown(Level.Invocation)
    public void tearDown() throws Exception {
      map.close();
    }
  }

  /** Reads the nodes of one way; the time is per node. */
  @Benchmark
  @OperationsPerInvocation(NODES_PER_WAY)
  public double wayLookup(Filled state) {
    int start = state.starts[state.way++ & WAY_MASK];
    double sum = 0;
    for (int i = 0; i < NODES_PER_WAY; i++) {
      sum += state.map.get(state.ids[start + i]).x;
    }
    return sum;
  }

  /** Probes an id that may or may not exist, one in four does not. */
  @Benchmark
  public Coordinate anyIdLookup(Filled state) {
    long id = state.starts[state.way++ & WAY_MASK] & (ID_SPACE - 1);
    return state.map.get(id);
  }

  /** Fills a fresh index with every node of the space, in order; the time is per node. */
  @Benchmark
  @OperationsPerInvocation(ID_SPACE * 3 / 4)
  @Warmup(iterations = 2)
  @Measurement(iterations = 3)
  public DataMap<Long, Coordinate> fill(Empty state) {
    for (long id : state.ids) {
      state.map.put(id, new Coordinate(id % 360 - 180, id % 180 - 90));
    }
    return state.map;
  }
}
