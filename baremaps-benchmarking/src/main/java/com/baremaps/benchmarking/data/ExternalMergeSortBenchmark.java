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

import com.baremaps.data.algorithm.ExternalMergeSort;
import com.baremaps.data.collection.DataList;
import com.baremaps.data.collection.FixedSizeDataList;
import com.baremaps.data.type.LongDataType;
import java.util.Comparator;
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

/** Sorting a million longs with various batch sizes; the time is for the whole sort. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
public class ExternalMergeSortBenchmark {

  private static final int COUNT = 1 << 20;

  @Param({"10000", "100000", "1000000"})
  public long batchSize;

  private DataList<Long> input;

  private DataList<Long> output;

  @Setup
  public void setup() {
    var random = new Random(0);
    input = new FixedSizeDataList<>(new LongDataType());
    for (int i = 0; i < COUNT; i++) {
      input.add(random.nextLong());
    }
  }

  @Setup(Level.Invocation)
  public void setupOutput() {
    output = new FixedSizeDataList<>(new LongDataType());
  }

  @TearDown(Level.Invocation)
  public void tearDownOutput() throws Exception {
    output.close();
  }

  @Benchmark
  public DataList<Long> sort() {
    ExternalMergeSort.sort(input, output, Comparator.naturalOrder(),
        () -> new FixedSizeDataList<>(new LongDataType()), batchSize, false, true);
    return output;
  }
}
