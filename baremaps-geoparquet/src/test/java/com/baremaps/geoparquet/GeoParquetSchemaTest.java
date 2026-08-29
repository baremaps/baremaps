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

import com.baremaps.geoparquet.GeoParquetSchema.Cardinality;
import com.baremaps.geoparquet.GeoParquetSchema.Type;
import java.util.List;
import java.util.Map;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Types;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class GeoParquetSchemaTest {

  /** A file that declares no GeoParquet metadata is still a readable Parquet file. */
  @Test
  void plainParquetFileHasNoMetadata() {
    assertNull(GeoParquetMetadata.read(Map.of()));

    MessageType schema = Types.buildMessage()
        .optional(PrimitiveTypeName.BINARY).as(LogicalTypeAnnotation.stringType()).named("name")
        .optional(PrimitiveTypeName.BINARY).named("geometry")
        .named("Schema");

    GeoParquetSchema geoParquetSchema = GeoParquetSchema.of(schema, null);
    assertEquals(Type.STRING, geoParquetSchema.fields().get(0).type());
    // Without metadata, nothing tells a geometry column from any other binary column.
    assertEquals(Type.BINARY, geoParquetSchema.fields().get(1).type());
  }

  @Test
  void metadataMakesAColumnAGeometry() {
    MessageType schema = Types.buildMessage()
        .optional(PrimitiveTypeName.BINARY).named("geometry")
        .named("Schema");
    GeoParquetMetadata metadata = new GeoParquetMetadata("1.0.0", "geometry",
        Map.of("geometry", new GeoParquetMetadata.Column("WKB", null, null, null, null, null)),
        null, null, null, null, null, null, null);

    GeoParquetSchema geoParquetSchema = GeoParquetSchema.of(schema, metadata);
    assertEquals(Type.GEOMETRY, geoParquetSchema.fields().get(0).type());
    assertEquals(Cardinality.OPTIONAL, geoParquetSchema.fields().get(0).cardinality());
  }

  /** A group named bbox that does not hold the four bounds is an ordinary group. */
  @Test
  void bboxWithoutBoundsIsAGroup() {
    MessageType schema = Types.buildMessage()
        .optionalGroup()
        .optional(PrimitiveTypeName.DOUBLE).named("xmin")
        .optional(PrimitiveTypeName.DOUBLE).named("ymin")
        .named("bbox")
        .named("Schema");
    assertEquals(Type.GROUP, GeoParquetSchema.of(schema, null).fields().get(0).type());
  }

  @Test
  void bboxWithBoundsInAnyOrderIsAnEnvelope() {
    MessageType schema = Types.buildMessage()
        .optionalGroup()
        .optional(PrimitiveTypeName.DOUBLE).named("xmax")
        .optional(PrimitiveTypeName.DOUBLE).named("xmin")
        .optional(PrimitiveTypeName.DOUBLE).named("ymax")
        .optional(PrimitiveTypeName.DOUBLE).named("ymin")
        .named("bbox")
        .named("Schema");
    assertEquals(Type.ENVELOPE, GeoParquetSchema.of(schema, null).fields().get(0).type());
  }

  @Test
  void metadataExposesTheBoundingBoxOfTheFile() {
    GeoParquetMetadata metadata = new GeoParquetMetadata("1.0.0", "geometry", Map.of(), null, null,
        null, null, List.of(-10.0, -20.0, 30.0, 40.0), null, null);
    assertEquals(new Envelope(-10.0, 30.0, -20.0, 40.0), metadata.envelope());
  }

  @Test
  void metadataWithoutBoundingBoxHasNoEnvelope() {
    GeoParquetMetadata metadata = new GeoParquetMetadata("1.0.0", "geometry", Map.of(), null, null,
        null, null, null, null, null);
    assertNull(metadata.envelope());
  }
}
