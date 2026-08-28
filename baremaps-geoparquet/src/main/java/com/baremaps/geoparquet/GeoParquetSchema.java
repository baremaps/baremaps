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

/**
 * A GeoParquet schema that describes the fields of a group and can easily be introspected.
 *
 * @param name
 * @param fields the fields of the schema
 */
public record GeoParquetSchema(String name, List<Field> fields) {

  /**
   * The type of a GeoParquet field.
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
   * A sealed inteface for the fields of a GeoParquet schema.
   * <p>
   * Sealed interfaces were introduced in Java 17 and can be used with pattern matching since Java
   * 21.
   */
  public sealed
  interface Field {
    String name();

    Type type();

    Cardinality cardinality();
  }

  record BinaryField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.BINARY;
      }
  }

  record BooleanField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.BOOLEAN;
      }
  }

  record DoubleField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.DOUBLE;
      }
  }

  record FloatField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.FLOAT;
      }
  }

  record IntegerField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.INTEGER;
      }
  }

  record Int96Field(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.INT96;
      }
  }

  record LongField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.LONG;
      }
  }

  record StringField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.STRING;
      }
  }

  record GeometryField(String name, Cardinality cardinality) implements Field {

    @Override
      public Type type() {
        return Type.GEOMETRY;
      }
  }

  record EnvelopeField(String name, Cardinality cardinality,
      GeoParquetSchema schema) implements Field {

    @Override
      public Type type() {
        return Type.ENVELOPE;
      }
  }

  public record GroupField(String name, Cardinality cardinality,
      GeoParquetSchema schema) implements Field {

    @Override
    public Type type() {
      return Type.GROUP;
    }
  }
}
