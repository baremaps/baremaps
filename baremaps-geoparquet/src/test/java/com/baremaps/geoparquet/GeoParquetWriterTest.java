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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.geoparquet.GeoParquetMetadata.Column;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  @Test
  @Tag("integration")
  void writeAndReadValues() throws IOException {
    MessageType schema = Types.buildMessage()
        .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("city")
        .optional(PrimitiveTypeName.BINARY).named("geometry")
        .named("GeoParquetSchema");
    GeoParquetMetadata metadata = metadata(Map.of("geometry",
        new Column("WKB", List.of("Point"), null, null, null, null)));
    Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(1.0, 2.0));

    Path path = path("geoparquet-values.parquet");
    try {
      write(path, schema, metadata, group -> {
        group.add("name", "Test Point");
        group.add("city", "Test City");
        group.add("geometry", point);
      });

      GeoParquetGroup group = readFirst(path);
      assertEquals("Test Point", group.getValue("name"));
      assertEquals("Test City", group.getValue("city"));
      assertTrue(point.equalsExact((Point) group.getValue("geometry")));
    } finally {
      delete(path);
    }
  }

  /**
   * A repeated field must survive the round trip: the record consumer records the repetition levels
   * from the way the values are announced, not from their number.
   */
  @Test
  @Tag("integration")
  void writeAndReadRepeatedValues() throws IOException {
    MessageType schema = Types.buildMessage()
        .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        .repeated(PrimitiveTypeName.INT32).named("codes")
        .named("GeoParquetSchema");

    Path path = path("geoparquet-repeated.parquet");
    try {
      write(path, schema, metadata(Map.of()), group -> {
        group.add("name", "first");
        group.add("codes", 1);
        group.add("codes", 2);
        group.add("codes", 3);
      });

      GeoParquetGroup group = readFirst(path);
      assertEquals("first", group.getValue("name"));
      assertEquals(List.of(1, 2, 3), group.getValues("codes"));
    } finally {
      delete(path);
    }
  }

  /** A repeated group is the shape real GeoParquet data uses for a list of structs. */
  @Test
  @Tag("integration")
  void writeAndReadRepeatedGroups() throws IOException {
    MessageType schema = Types.buildMessage()
        .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        .repeatedGroup()
        .required(PrimitiveTypeName.INT32).named("code")
        .named("codes")
        .named("GeoParquetSchema");

    Path path = path("geoparquet-repeated-groups.parquet");
    try {
      write(path, schema, metadata(Map.of()), group -> {
        group.add("name", "first");
        group.addGroup("codes").add("code", 1);
        group.addGroup("codes").add("code", 2);
        group.addGroup("codes").add("code", 3);
      });

      GeoParquetGroup group = readFirst(path);
      assertEquals(
          List.of(1, 2, 3),
          group.getValues("codes").stream()
              .map(code -> ((GeoParquetGroup) code).getValue("code"))
              .toList());
    } finally {
      delete(path);
    }
  }

  /** An absent optional field reads back as a null value and as an empty list of values. */
  @Test
  @Tag("integration")
  void writeAndReadNestedAndAbsentValues() throws IOException {
    MessageType schema = Types.buildMessage()
        .required(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        .optional(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("comment")
        .optionalGroup()
        .required(PrimitiveTypeName.DOUBLE).named("x")
        .optional(PrimitiveTypeName.DOUBLE).named("y")
        .named("location")
        .named("GeoParquetSchema");

    Path path = path("geoparquet-nested.parquet");
    try {
      write(path, schema, metadata(Map.of()), group -> {
        group.add("name", "first");
        group.addGroup("location").add("x", 1.5);
      });

      GeoParquetGroup group = readFirst(path);
      assertNull(group.getValue("comment"));
      assertEquals(List.of(), group.getValues("comment"));

      GeoParquetGroup location = (GeoParquetGroup) group.getValue("location");
      assertEquals(1.5, location.getValue("x"));
      assertNull(location.getValue("y"));
    } finally {
      delete(path);
    }
  }

  @Test
  @Tag("integration")
  void copyGeoParquetData() throws IOException {
    Path source = new Path(TestFiles.GEOPARQUET.toUri());
    GeoParquetReader reader = new GeoParquetReader(source);

    Path path = path("geoparquet-copy.parquet");
    try {
      try (ParquetWriter<GeoParquetGroup> writer = GeoParquetWriter.builder(path)
          .withType(reader.getParquetSchema())
          .withGeoParquetMetadata(reader.getGeoParquetMetadata())
          .build();
          Stream<GeoParquetGroup> groups = reader.read()) {
        for (GeoParquetGroup group : (Iterable<GeoParquetGroup>) groups::iterator) {
          writer.write(group);
        }
      }

      GeoParquetReader copy = new GeoParquetReader(path);
      assertEquals(5, copy.size());
      assertEquals(reader.getGeoParquetSchema(), copy.getGeoParquetSchema());
      assertEquals("Fiji", readFirst(path).getValue("name"));
    } finally {
      delete(path);
    }
  }

  private static GeoParquetMetadata metadata(Map<String, Column> columns) {
    return new GeoParquetMetadata(
        "1.0.0", "geometry", columns, null, null, null, null, null, null, null);
  }

  private static Path path(String name) {
    return new Path("target/test-output/" + name);
  }

  private static void write(
      Path path,
      MessageType schema,
      GeoParquetMetadata metadata,
      java.util.function.Consumer<GeoParquetGroup> fill) throws IOException {
    try (ParquetWriter<GeoParquetGroup> writer = GeoParquetWriter.builder(path)
        .withType(schema)
        .withGeoParquetMetadata(metadata)
        .build()) {
      GeoParquetGroup group =
          new GeoParquetGroup(schema, GeoParquetSchema.of(schema, metadata));
      fill.accept(group);
      writer.write(group);
    }
  }

  private static GeoParquetGroup readFirst(Path path) {
    try (Stream<GeoParquetGroup> groups = new GeoParquetReader(path).read()) {
      return groups.findFirst().orElseThrow();
    }
  }

  private static void delete(Path path) throws IOException {
    path.getFileSystem(new Configuration()).delete(path, false);
  }
}
