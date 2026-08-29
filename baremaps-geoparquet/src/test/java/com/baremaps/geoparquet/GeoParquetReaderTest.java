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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.geoparquet.GeoParquetSchema.Type;
import com.baremaps.testing.TestFiles;
import java.util.List;
import java.util.stream.Stream;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

class GeoParquetReaderTest {

  private static final Path GEOPARQUET = new Path(TestFiles.GEOPARQUET.toUri());

  @Test
  void read() {
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET).read()) {
      assertEquals(5, groups.count());
    }
  }

  @Test
  void readParallel() {
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET).readParallel()) {
      assertEquals(5, groups.count());
    }
  }

  /**
   * The envelope covers North America. Canada and the United States intersect it; the United States
   * only partly, which a filter comparing the wrong way round would reject.
   */
  @Test
  void readFiltered() {
    Envelope envelope = new Envelope(-172, -65, 18, 72);
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET, envelope).read()) {
      assertEquals(
          List.of("Canada", "United States of America"),
          groups.map(group -> group.getValue("name")).toList());
    }
  }

  /** An envelope that contains a record entirely must not reject it. */
  @Test
  void readFilteredByContainingEnvelope() {
    Envelope envelope = new Envelope(-170, 170, -80, 80);
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET, envelope).read()) {
      assertEquals(5, groups.count());
    }
  }

  /** An envelope that matches nothing must yield nothing rather than everything. */
  @Test
  void readFilteredByDisjointEnvelope() {
    Envelope envelope = new Envelope(100, 120, -80, -70);
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET, envelope).read()) {
      assertEquals(0, groups.count());
    }
  }

  @Test
  void size() {
    assertEquals(5, new GeoParquetReader(GEOPARQUET).size());
  }

  @Test
  void validateSchemas() {
    assertTrue(new GeoParquetReader(GEOPARQUET).validateSchemasAreIdentical());
  }

  @Test
  void schema() {
    GeoParquetSchema schema = new GeoParquetReader(GEOPARQUET).getGeoParquetSchema();
    assertEquals(
        List.of("pop_est", "continent", "name", "iso_a3", "gdp_md_est", "geometry", "bbox"),
        schema.fields().stream().map(GeoParquetSchema.Field::name).toList());
    assertEquals(Type.DOUBLE, schema.fields().get(0).type());
    assertEquals(Type.STRING, schema.fields().get(1).type());
    assertEquals(Type.LONG, schema.fields().get(4).type());
    assertEquals(Type.GEOMETRY, schema.fields().get(5).type());
    assertEquals(Type.ENVELOPE, schema.fields().get(6).type());
  }

  @Test
  void readValues() {
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET).read()) {
      GeoParquetGroup group = groups.findFirst().orElseThrow();
      assertEquals("Fiji", group.getValue("name"));
      assertEquals("Oceania", group.getValue("continent"));
      assertEquals(889953.0, group.getValue("pop_est"));
      assertEquals(5496L, group.getValue("gdp_md_est"));
      assertNotNull((Geometry) group.getValue("geometry"));
    }
  }

  /**
   * The bounds of a bounding box are matched by name: this file stores them as xmax, xmin, ymax,
   * ymin, so reading them by position would swap them.
   */
  @Test
  void readEnvelope() {
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(GEOPARQUET).read()) {
      GeoParquetGroup canada = groups.skip(3).findFirst().orElseThrow();
      assertEquals("Canada", canada.getValue("name"));
      Envelope envelope = (Envelope) canada.getValue("bbox");
      assertEquals(-140.99778, envelope.getMinX(), 1e-9);
      assertEquals(-52.648098720, envelope.getMaxX(), 1e-9);
      assertEquals(41.675105088, envelope.getMinY(), 1e-9);
      assertEquals(83.233240000, envelope.getMaxY(), 1e-9);
    }
  }
}
