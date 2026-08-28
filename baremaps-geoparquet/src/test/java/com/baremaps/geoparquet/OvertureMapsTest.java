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

package com.baremaps.geoparquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class OvertureMapsTest {

  @Disabled("Requires access to the Internet")
  @Test
  void countAddressesInSwitzerland() {
    Path geoParquet =
        new Path("s3a://overturemaps-us-west-2/release/2024-09-18.0/theme=addresses/**/*.parquet");
    Envelope switzerland = new Envelope(6.02260949059, 10.4427014502, 45.7769477403, 47.8308275417);
    GeoParquetReader geoParquetReader =
        new GeoParquetReader(geoParquet, switzerland, OvertureMaps.configuration());
    assertEquals(10397434, geoParquetReader.readParallel().count());
  }

  @Disabled("Requires access to the Internet")
  @Test
  void validateSchemas() {
    Path geoParquet =
        new Path("s3a://overturemaps-us-west-2/release/2024-09-18.0/theme=addresses/**/*.parquet");
    GeoParquetReader geoParquetReader =
        new GeoParquetReader(geoParquet, null, OvertureMaps.configuration());
    assertTrue(geoParquetReader.validateSchemasAreIdentical(), "Schemas are identical");
  }

  @Disabled("Requires access to the Internet")
  @Test
  void size() {
    Path geoParquet =
        new Path("s3a://overturemaps-us-west-2/release/2024-09-18.0/theme=addresses/**/*.parquet");
    GeoParquetReader geoParquetReader =
        new GeoParquetReader(geoParquet, null, OvertureMaps.configuration());
    assertEquals(213535887L, geoParquetReader.size());
  }

}
