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

package com.baremaps.calcite.geoparquet;

import com.baremaps.geoparquet.GeoParquetGroup;
import com.baremaps.geoparquet.GeoParquetSchema;
import com.baremaps.geoparquet.GeoParquetSchema.Cardinality;
import com.baremaps.geoparquet.GeoParquetSchema.Field;
import com.baremaps.geoparquet.GeoParquetSchema.Type;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

/** Converts GeoParquet schemas and groups into Calcite row types and rows. */
final class GeoParquetTypeConversion {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private GeoParquetTypeConversion() {}

  static RelDataType toRelDataType(RelDataTypeFactory typeFactory, GeoParquetSchema schema) {
    RelDataTypeFactory.Builder builder = typeFactory.builder();
    for (Field field : schema.fields()) {
      builder.add(field.name(), toRelDataType(typeFactory, field));
    }
    return builder.build();
  }

  private static RelDataType toRelDataType(RelDataTypeFactory typeFactory, Field field) {
    return switch (field.type()) {
      // INT96 is a deprecated timestamp encoding that GeoParquet hands over as raw bytes.
      case BINARY, INT96 -> typeFactory.createSqlType(SqlTypeName.VARBINARY);
      case BOOLEAN -> typeFactory.createSqlType(SqlTypeName.BOOLEAN);
      case INTEGER -> typeFactory.createSqlType(SqlTypeName.INTEGER);
      case LONG -> typeFactory.createSqlType(SqlTypeName.BIGINT);
      case FLOAT -> typeFactory.createSqlType(SqlTypeName.FLOAT);
      case DOUBLE -> typeFactory.createSqlType(SqlTypeName.DOUBLE);
      case STRING -> typeFactory.createSqlType(SqlTypeName.VARCHAR);
      case GEOMETRY, ENVELOPE -> typeFactory.createSqlType(SqlTypeName.GEOMETRY);
      case GROUP -> typeFactory.createSqlType(SqlTypeName.VARCHAR); // JSON, see toRow
    };
  }

  /**
   * Converts a group into a row. Nested groups become JSON strings: Calcite has no natural Java
   * representation for struct values that survives a {@code CREATE TABLE ... AS SELECT} into
   * another storage, whereas JSON does.
   */
  static Object[] toRow(GeoParquetGroup group) {
    List<Field> fields = group.getGeoParquetSchema().fields();
    Object[] row = new Object[fields.size()];
    for (int i = 0; i < fields.size(); i++) {
      Field field = fields.get(i);
      Object value = value(field, group, i);
      row[i] = field.type() == Type.GROUP ? toJson(value) : value;
    }
    return row;
  }

  private static String toJson(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Object value(Field field, GeoParquetGroup group, int index) {
    if (field.cardinality() == Cardinality.REPEATED) {
      return group.getValues(index).stream().map(GeoParquetTypeConversion::toCalciteValue).toList();
    }
    return toCalciteValue(group.getValue(index));
  }

  /**
   * The group already decodes values according to its schema; only the two that have no Calcite
   * counterpart, envelopes and nested groups, are converted further.
   */
  private static Object toCalciteValue(Object value) {
    if (value instanceof Envelope envelope) {
      return GEOMETRY_FACTORY.toGeometry(envelope);
    }
    if (value instanceof GeoParquetGroup group) {
      return toMap(group);
    }
    return value;
  }

  private static Map<String, Object> toMap(GeoParquetGroup group) {
    Map<String, Object> map = new HashMap<>();
    List<Field> fields = group.getGeoParquetSchema().fields();
    for (int i = 0; i < fields.size(); i++) {
      if (!group.getValues(i).isEmpty()) {
        map.put(fields.get(i).name(), value(fields.get(i), group, i));
      }
    }
    return map;
  }
}
