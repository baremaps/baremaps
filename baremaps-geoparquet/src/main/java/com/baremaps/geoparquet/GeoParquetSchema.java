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

import java.util.List;
import java.util.Map;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;

/**
 * The schema of a {@link GeoParquetGroup}, derived from the Parquet schema of a file and from its
 * GeoParquet metadata.
 * <p>
 * The Parquet schema alone cannot tell a geometry from an ordinary binary column, nor a bounding
 * box from an ordinary group: both distinctions come from the metadata. Resolving them once, here,
 * keeps that knowledge out of the read and write paths.
 *
 * @param name the name of the group
 * @param fields the fields of the schema
 */
public record GeoParquetSchema(String name, List<Field> fields) {

  /** The bounds a group must expose, by name, to be read as an {@link Type#ENVELOPE}. */
  private static final List<String> BOUNDS = List.of("xmin", "ymin", "xmax", "ymax");

  /**
   * The type of a GeoParquet field, i.e. the kind of value {@link GeoParquetGroup#getValue(int)}
   * returns for it.
   */
  public enum Type {
    BINARY,
    BOOLEAN,
    DOUBLE,
    FLOAT,
    INTEGER,
    INT96,
    LONG,
    STRING,
    GEOMETRY,
    ENVELOPE,
    GROUP
  }

  /**
   * The cardinality of a GeoParquet field.
   */
  public enum Cardinality {
    REQUIRED,
    OPTIONAL,
    REPEATED
  }

  /**
   * A field of a GeoParquet schema.
   */
  public sealed
  interface Field {

    String name();

    Type type();

    Cardinality cardinality();
  }

  /**
   * A field holding a single value.
   */
  public record PrimitiveField(String name, Type type, Cardinality cardinality) implements Field {
  }

  /**
   * A field holding a nested group of fields, either an {@link Type#ENVELOPE} or a plain
   * {@link Type#GROUP}.
   */
  public record GroupField(String name, Type type, Cardinality cardinality,
      GeoParquetSchema schema) implements Field {

    public GroupField
    {
      if (type != Type.GROUP && type != Type.ENVELOPE) {
        throw new GeoParquetException("A group field cannot be of type " + type + ".");
      }
    }
  }

  /**
   * Derives the schema of a group from its Parquet type and the metadata of the file it belongs to.
   *
   * @param groupType the Parquet type of the group
   * @param metadata the metadata of the file, which may be null for a plain Parquet file
   * @return the schema
   */
  public static GeoParquetSchema of(GroupType groupType, GeoParquetMetadata metadata) {
    Map<String, GeoParquetMetadata.Column> columns =
        metadata == null || metadata.columns() == null ? Map.of() : metadata.columns();
    List<Field> fields = groupType.getFields().stream()
        .map(field -> field(field, columns, metadata))
        .toList();
    return new GeoParquetSchema(groupType.getName(), fields);
  }

  private static Field field(
      org.apache.parquet.schema.Type field,
      Map<String, GeoParquetMetadata.Column> columns,
      GeoParquetMetadata metadata) {
    String name = field.getName();
    Cardinality cardinality = switch (field.getRepetition()) {
      case REQUIRED -> Cardinality.REQUIRED;
      case OPTIONAL -> Cardinality.OPTIONAL;
      case REPEATED -> Cardinality.REPEATED;
    };
    if (field.isPrimitive()) {
      Type type = columns.containsKey(name) ? Type.GEOMETRY : type(field.asPrimitiveType());
      return new PrimitiveField(name, type, cardinality);
    }
    GroupType groupType = field.asGroupType();
    Type type = isEnvelope(groupType) ? Type.ENVELOPE : Type.GROUP;
    return new GroupField(name, type, cardinality, of(groupType, metadata));
  }

  private static Type type(PrimitiveType primitiveType) {
    if (LogicalTypeAnnotation.stringType().equals(primitiveType.getLogicalTypeAnnotation())) {
      return Type.STRING;
    }
    return switch (primitiveType.getPrimitiveTypeName()) {
      case INT32 -> Type.INTEGER;
      case INT64 -> Type.LONG;
      case INT96 -> Type.INT96;
      case FLOAT -> Type.FLOAT;
      case DOUBLE -> Type.DOUBLE;
      case BOOLEAN -> Type.BOOLEAN;
      case BINARY, FIXED_LEN_BYTE_ARRAY -> Type.BINARY;
    };
  }

  /**
   * Tells whether a group is the bounding box that GeoParquet suggests writing alongside a geometry
   * column. The bounds are matched by name and never by position: writers are free to order them as
   * they like, and the reference example file stores them as xmax, xmin, ymax, ymin.
   */
  private static boolean isEnvelope(GroupType groupType) {
    return "bbox".equals(groupType.getName()) && BOUNDS.stream().allMatch(bound -> isBound(groupType, bound));
  }

  private static boolean isBound(GroupType groupType, String bound) {
    if (!groupType.containsField(bound) || !groupType.getType(bound).isPrimitive()) {
      return false;
    }
    return switch (groupType.getType(bound).asPrimitiveType().getPrimitiveTypeName()) {
      case FLOAT, DOUBLE -> true;
      default -> false;
    };
  }
}
