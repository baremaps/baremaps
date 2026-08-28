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

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.testing.TestFiles;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class GeoParquetReaderTest {

  @Test
  void read() {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader geoParquetReader = new GeoParquetReader(geoParquet);
    assertEquals(5, geoParquetReader.read().count());
  }

  @Test
  void readFiltered() {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader geoParquetReader =
        new GeoParquetReader(geoParquet, new Envelope(-172, -65, 18, 72));
    assertEquals(1, geoParquetReader.read().count());
  }

  @Test
  void size() {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader geoParquetReader = new GeoParquetReader(geoParquet);
    assertEquals(5, geoParquetReader.size());
  }

  @Test
  void count() {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader geoParquetReader = new GeoParquetReader(geoParquet);
    assertEquals(5, geoParquetReader.read().count());
  }

  @Test
  void validateSchemas() {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader geoParquetReader = new GeoParquetReader(geoParquet);
    assertTrue(geoParquetReader.validateSchemasAreIdentical());
  }
}
