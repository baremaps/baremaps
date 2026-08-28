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

import com.baremaps.geoparquet.GeoParquetMetadata.Column;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Types;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class GeoParquetWriterTest {

  @Test
  @Tag("integration")
  void testWriteAndReadGeoParquet() throws IOException {
    // Create the output file
    Configuration conf = new Configuration();
    Path outputPath = new Path("target/test-output/geoparquet-test.parquet");

    try {
      // Define the Parquet schema
      MessageType type = Types.buildMessage()
          .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
          .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("city")
          .optional(PrimitiveTypeName.BINARY).named("geometry")
          .named("GeoParquetSchema");

      // Create GeoParquet metadata
      Map<String, Column> columns = new HashMap<>();
      columns.put("geometry", new GeoParquetMetadata.Column(
          "WKB",
          List.of("Point"),
          null,
          null,
          null,
          null));

      GeoParquetMetadata metadata = new GeoParquetMetadata(
          "1.0",
          "geometry",
          columns,
          null,
          null,
          null,
          null,
          null,
          null,
          null);

      // Create a Point geometry
      GeometryFactory geometryFactory = new GeometryFactory();
      Point point = geometryFactory.createPoint(new Coordinate(1.0, 2.0));

      // Create the GeoParquetWriter
      try (ParquetWriter<GeoParquetGroup> writer = GeoParquetWriter.builder(outputPath)
          .withType(type)
          .withGeoParquetMetadata(metadata)
          .build()) {

        // Create a GeoParquetGroup and write it
        GeoParquetSchema geoParquetSchema =
            GeoParquetGroupFactory.createGeoParquetSchema(type, metadata);
        GeoParquetGroup group =
            new GeoParquetGroup(type.asGroupType(), metadata, geoParquetSchema);
        group.add("name", "Test Point");
        group.add("city", "Test City");
        group.add("geometry", point);

        // Write the group
        writer.write(group);
      }

      // Now read back the file using GeoParquetReader
      GeoParquetReader reader = new GeoParquetReader(outputPath, null, conf);
      GeoParquetGroup readGroup = reader.read().findFirst().orElse(null);

      assertNotNull(readGroup, "Read group should not be null");

      // Verify the data
      assertEquals("Test Point", readGroup.getStringValue("name"));
      assertEquals("Test City", readGroup.getStringValue("city"));

      Point readPoint = (Point) readGroup.getGeometryValue("geometry");
      assertEquals(point.getX(), readPoint.getX(), 0.0001);
      assertEquals(point.getY(), readPoint.getY(), 0.0001);
    } finally {
      outputPath.getFileSystem(conf).delete(outputPath, false);
    }
  }

  @Test
  @Tag("integration")
  void copyGeoParquetData() throws IOException {
    Path geoParquet = new Path(TestFiles.GEOPARQUET.toUri());

    Configuration conf = new Configuration();
    Path outputPath = new Path("target/test-output/geoparquet-copy.parquet");

    try {
      // Write the GeoParquet file
      GeoParquetReader reader = new GeoParquetReader(geoParquet, null, conf);
      GeoParquetWriter.Builder builder = GeoParquetWriter.builder(outputPath);
      ParquetWriter<GeoParquetGroup> writer = builder.withType(reader.getParquetSchema())
          .withGeoParquetMetadata(reader.getGeoParquetMetadata()).build();
      Iterator<GeoParquetGroup> iterator = reader.read().iterator();
      while (iterator.hasNext()) {
        writer.write(iterator.next());
      }
      writer.close();

      // Read the copied file
      GeoParquetReader copiedReader = new GeoParquetReader(outputPath, null, conf);
      assertEquals(5, copiedReader.read().count());
    } finally {
      outputPath.getFileSystem(conf).delete(outputPath, false);
    }



  }

}
