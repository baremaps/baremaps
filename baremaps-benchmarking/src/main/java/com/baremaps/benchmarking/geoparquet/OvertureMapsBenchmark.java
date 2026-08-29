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

package com.baremaps.benchmarking.geoparquet;

import com.baremaps.geoparquet.GeoParquetReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.fs.s3a.AnonymousAWSCredentialsProvider;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
public class OvertureMapsBenchmark {

  private static Path directory = Path.of("baremaps-benchmarking/data/overturemaps");

  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(OvertureMapsBenchmark.class.getSimpleName())
        .forks(1)
        .build();
    new Runner(opt).run();
  }

  @Setup
  public void setup() throws IOException {
    if (!Files.exists(directory)) {
      try (var client = S3Client.builder()
          .region(Region.US_EAST_1)
          .credentialsProvider(new AnonymousAWSCredentialsProvider())
          .build()) {

        var listRequest = ListObjectsV2Request.builder()
            .bucket("overturemaps-us-west-2")
            .prefix("release/2024-09-18.0/theme=addresses/")
            .build();
        var objects = client.listObjectsV2(listRequest).contents();
        for (var object : objects) {
          var key = object.key();
          var name = key.substring(key.lastIndexOf("/") + 1);
          var file = directory.resolve(name);
          Files.createDirectories(file.getParent());
          if (!Files.exists(file)) {
            var getRequest = GetObjectRequest.builder()
                .bucket("overturemaps-us-west-2")
                .key(key)
                .build();
            client.getObject(getRequest, file);
          }
        }
      }
    }
  }

  @SuppressWarnings({"squid:S1481", "squid:S2201"})
  @Benchmark
  public void read() {
    var path = new org.apache.hadoop.fs.Path(directory.toUri());
    try (var groups = new GeoParquetReader(path).read()) {
      groups.count();
    }
  }

  @SuppressWarnings({"squid:S1481", "squid:S2201"})
  @Benchmark
  public void readParallel() {
    var path = new org.apache.hadoop.fs.Path(directory.toUri());
    try (var groups = new GeoParquetReader(path).readParallel()) {
      groups.count();
    }
  }
}
