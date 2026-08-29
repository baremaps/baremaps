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

import com.baremaps.data.type.DataType;
import com.baremaps.data.type.GeometryDataType;
import com.baremaps.data.type.LonLatDataType;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.LongListDataType;
import com.baremaps.data.type.StringDataType;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
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
import org.openjdk.jmh.annotations.Warmup;

/** Encoding and decoding one value per data type, in a heap buffer. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class DataTypeBenchmark {

  @Param({"long", "string", "longlist", "lonlat", "point", "linestring"})
  public String kind;

  private DataType<Object> type;

  private Object value;

  private ByteBuffer buffer;

  @Setup
  @SuppressWarnings("unchecked")
  public void setup() {
    var factory = new GeometryFactory();
    switch (kind) {
      case "long" -> {
        type = (DataType<Object>) (DataType<?>) new LongDataType();
        value = 123456789L;
      }
      case "string" -> {
        type = (DataType<Object>) (DataType<?>) new StringDataType();
        value = "a twenty character s";
      }
      case "longlist" -> {
        type = (DataType<Object>) (DataType<?>) new LongListDataType();
        value = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
      }
      case "lonlat" -> {
        type = (DataType<Object>) (DataType<?>) new LonLatDataType();
        value = new Coordinate(6.6323, 46.5197);
      }
      case "point" -> {
        type = (DataType<Object>) (DataType<?>) new GeometryDataType(factory);
        value = factory.createPoint(new Coordinate(6.6323, 46.5197));
      }
      case "linestring" -> {
        type = (DataType<Object>) (DataType<?>) new GeometryDataType(factory);
        var coordinates = new Coordinate[10];
        for (int i = 0; i < coordinates.length; i++) {
          coordinates[i] = new Coordinate(i, i * 2);
        }
        value = factory.createLineString(coordinates);
      }
      default -> throw new IllegalArgumentException(kind);
    }
    buffer = ByteBuffer.allocate(1 << 10);
    type.write(buffer, 0, value);
  }

  @Benchmark
  public void write() {
    type.write(buffer, 0, value);
  }

  @Benchmark
  public Object read() {
    return type.read(buffer, 0);
  }

  @Benchmark
  public int size() {
    return type.size(value);
  }
}
