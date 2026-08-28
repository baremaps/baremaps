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

import com.baremaps.geoparquet.GeoParquetSchema.*;
import java.util.List;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

/**
 * A factory for creating {@link GeoParquetGroup}s.
 */
class GeoParquetGroupFactory {

  private final GroupType schema;

  private final GeoParquetMetadata metadata;

  private final GeoParquetSchema geoParquetSchema;

  /**
   * Constructs a new {@code GeoParquetGroupFactory} with the specified schema and metadata.
   *
   * @param schema the schema
   * @param metadata the metadata
   */
  public GeoParquetGroupFactory(GroupType schema, GeoParquetMetadata metadata) {
    this.schema = schema;
    this.metadata = metadata;
    this.geoParquetSchema = createGeoParquetSchema(schema, metadata);
  }

  /**
   * Creates a {@link GeoParquetSchema} from a {@link GroupType} and a {@link GeoParquetMetadata}.
   *
   * @param schema the schema
   * @param metadata the metadata
   * @return the schema
   */
  public static GeoParquetSchema createGeoParquetSchema(
      GroupType schema,
      GeoParquetMetadata metadata) {

    // Map the fields
    List<Field> fields = schema.getFields().stream().map(field -> {

      // Map the column cardinality
      Cardinality cardinality = switch (field.getRepetition()) {
        case REQUIRED -> GeoParquetSchema.Cardinality.REQUIRED;
        case OPTIONAL -> GeoParquetSchema.Cardinality.OPTIONAL;
        case REPEATED -> GeoParquetSchema.Cardinality.REPEATED;
      };

      // Handle geometry columns
      if (field.isPrimitive() && metadata.columns().containsKey(field.getName())) {
        return new GeometryField(field.getName(), cardinality);
      }

      // Handle envelope columns
      else if (!field.isPrimitive() && field.getName().equals("bbox")) {
        GroupType groupType = field.asGroupType();
        GeoParquetSchema geoParquetSchema = createGeoParquetSchema(groupType, metadata);
        return new EnvelopeField(field.getName(), cardinality, geoParquetSchema);
      }

      // Handle group columns
      else if (!field.isPrimitive()) {
        GroupType groupType = field.asGroupType();
        GeoParquetSchema geoParquetSchema = createGeoParquetSchema(groupType, metadata);
        return (Field) new GroupField(
            groupType.getName(),
            cardinality,
            geoParquetSchema);
      }

      // Handle logical types
      else if (field.getLogicalTypeAnnotation() != null
          && field.getLogicalTypeAnnotation().equals(LogicalTypeAnnotation.stringType())) {
        return new StringField(field.getName(), cardinality);
      }

      // Handle primitive columns
      else {
        PrimitiveType primitiveType = field.asPrimitiveType();
        PrimitiveTypeName primitiveTypeName = primitiveType.getPrimitiveTypeName();
        String columnName = primitiveType.getName();
        return switch (primitiveTypeName) {
          case INT32 -> new IntegerField(columnName, cardinality);
          case INT64 -> new LongField(columnName, cardinality);
          case INT96 -> new Int96Field(columnName, cardinality);
          case FLOAT -> new FloatField(columnName, cardinality);
          case DOUBLE -> new DoubleField(columnName, cardinality);
          case BOOLEAN -> new BooleanField(columnName, cardinality);
          case BINARY -> new BinaryField(columnName, cardinality);
          case FIXED_LEN_BYTE_ARRAY -> new BinaryField(columnName, cardinality);
        };
      }
    }).toList();

    return new GeoParquetSchema(schema.getName(), fields);
  }



  /**
   * Creates a new {@link GeoParquetGroup}.
   *
   * @return the group
   */
  public GeoParquetGroup newGroup() {
    return new GeoParquetGroup(schema, metadata, geoParquetSchema);
  }
}
