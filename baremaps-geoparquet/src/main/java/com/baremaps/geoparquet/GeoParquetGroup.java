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

import com.baremaps.geoparquet.GeoParquetSchema.Cardinality;
import com.baremaps.geoparquet.GeoParquetSchema.Field;
import com.baremaps.geoparquet.GeoParquetSchema.GroupField;
import java.util.ArrayList;
import java.util.List;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.GroupType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

/**
 * A group of fields in a GeoParquet file.
 * <p>
 * Values are stored the way Parquet hands them over, and decoded on the way out according to the
 * {@link GeoParquetSchema}: a geometry column becomes a {@link Geometry}, a bounding box group an
 * {@link Envelope}, a string column a {@link String}. Callers therefore need a single pair of
 * accessors, {@link #getValue(int)} and {@link #getValues(int)}, rather than one per Parquet type.
 */
public class GeoParquetGroup {

  private final GroupType parquetSchema;
  private final GeoParquetSchema geoParquetSchema;
  private final Object[] data;

  /**
   * Constructs an empty group.
   *
   * @param parquetSchema the Parquet type of the group
   * @param geoParquetSchema the schema derived from it, which must describe the same fields
   */
  public GeoParquetGroup(GroupType parquetSchema, GeoParquetSchema geoParquetSchema) {
    if (parquetSchema.getFieldCount() != geoParquetSchema.fields().size()) {
      throw new GeoParquetException(
          "The Parquet and GeoParquet schemas describe different fields.");
    }
    this.parquetSchema = parquetSchema;
    this.geoParquetSchema = geoParquetSchema;
    this.data = new Object[parquetSchema.getFieldCount()];
    for (int i = 0; i < data.length; i++) {
      // A repeated field accumulates its values in a list; the others hold a single value, or null
      // as long as they are absent.
      if (geoParquetSchema.fields().get(i).cardinality() == Cardinality.REPEATED) {
        data[i] = new ArrayList<>();
      }
    }
  }

  public GroupType getParquetSchema() {
    return parquetSchema;
  }

  public GeoParquetSchema getGeoParquetSchema() {
    return geoParquetSchema;
  }

  /**
   * Returns the index of a field, so that callers that read many groups can resolve a name once.
   *
   * @param fieldName the name of the field
   * @return the index of the field
   */
  public int getFieldIndex(String fieldName) {
    return parquetSchema.getFieldIndex(fieldName);
  }

  /**
   * Returns the value of a field that holds at most one value, decoded according to the schema, or
   * null when the field is absent.
   *
   * @param fieldIndex the index of the field
   * @return the value of the field
   */
  public Object getValue(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    if (field.cardinality() == Cardinality.REPEATED) {
      throw new GeoParquetException(
          "Field " + field.name() + " is repeated, use getValues instead.");
    }
    return decode(field, data[fieldIndex]);
  }

  public Object getValue(String fieldName) {
    return getValue(getFieldIndex(fieldName));
  }

  /**
   * Returns the values of a field, decoded according to the schema. Fields that hold at most one
   * value are returned as a list of zero or one element, so that callers can read repeated and non
   * repeated fields the same way.
   *
   * @param fieldIndex the index of the field
   * @return the values of the field
   */
  public List<Object> getValues(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    Object value = data[fieldIndex];
    if (value instanceof List<?>values) {
      return values.stream().map(element -> decode(field, element)).toList();
    }
    return value == null ? List.of() : List.of(decode(field, value));
  }

  public List<Object> getValues(String fieldName) {
    return getValues(getFieldIndex(fieldName));
  }

  private Object decode(Field field, Object value) {
    if (value == null) {
      return null;
    }
    return switch (field.type()) {
      case STRING -> ((Binary) value).toStringUsingUTF8();
      // INT96 is a deprecated timestamp encoding that Parquet hands over as twelve raw bytes.
      case BINARY, INT96 -> ((Binary) value).getBytes();
      case GEOMETRY -> geometry((Binary) value);
      case ENVELOPE -> envelope((GeoParquetGroup) value);
      case BOOLEAN, DOUBLE, FLOAT, INTEGER, LONG, GROUP -> value;
    };
  }

  private static Geometry geometry(Binary value) {
    try {
      return new WKBReader().read(value.getBytes());
    } catch (ParseException e) {
      throw new GeoParquetException("Failed to parse a WKB geometry.", e);
    }
  }

  private static Envelope envelope(GeoParquetGroup group) {
    Number xmin = group.bound("xmin");
    Number ymin = group.bound("ymin");
    Number xmax = group.bound("xmax");
    Number ymax = group.bound("ymax");
    if (xmin == null || ymin == null || xmax == null || ymax == null) {
      return null;
    }
    return new Envelope(
        xmin.doubleValue(), xmax.doubleValue(), ymin.doubleValue(), ymax.doubleValue());
  }

  private Number bound(String name) {
    return (Number) getValue(getFieldIndex(name));
  }

  /**
   * Appends an empty group to a field and returns it, so that its own fields can be filled in.
   *
   * @param fieldIndex the index of the field
   * @return the new group
   */
  public GeoParquetGroup addGroup(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    if (!(field instanceof GroupField groupField)) {
      throw new GeoParquetException("Field " + field.name() + " is not a group.");
    }
    GeoParquetGroup group =
        new GeoParquetGroup(parquetSchema.getType(fieldIndex).asGroupType(), groupField.schema());
    addValue(fieldIndex, group);
    return group;
  }

  public GeoParquetGroup addGroup(String fieldName) {
    return addGroup(getFieldIndex(fieldName));
  }

  public void add(int fieldIndex, int value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, long value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, float value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, double value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, boolean value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, Binary value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, String value) {
    addValue(fieldIndex, Binary.fromString(value));
  }

  public void add(int fieldIndex, Geometry value) {
    addValue(fieldIndex, Binary.fromConstantByteArray(new WKBWriter().write(value)));
  }

  public void add(int fieldIndex, GeoParquetGroup value) {
    addValue(fieldIndex, value);
  }

  public void add(String fieldName, int value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, long value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, float value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, double value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, boolean value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, Binary value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, String value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, Geometry value) {
    add(getFieldIndex(fieldName), value);
  }

  public void add(String fieldName, GeoParquetGroup value) {
    add(getFieldIndex(fieldName), value);
  }

  @SuppressWarnings("unchecked")
  private void addValue(int fieldIndex, Object value) {
    if (data[fieldIndex] instanceof List<?>values) {
      ((List<Object>) values).add(value);
    } else {
      data[fieldIndex] = value;
    }
  }

  /**
   * Returns how many values a field holds. Used by {@link GeoParquetWriteSupport}, which writes the
   * values back in the form Parquet gave them and therefore bypasses the decoding accessors.
   */
  int getFieldRepetitionCount(int fieldIndex) {
    Object value = data[fieldIndex];
    if (value instanceof List<?>values) {
      return values.size();
    }
    return value == null ? 0 : 1;
  }

  /**
   * Returns a value as it is stored, without decoding it.
   *
   * @see #getFieldRepetitionCount(int)
   */
  Object getRawValue(int fieldIndex, int index) {
    Object value = data[fieldIndex];
    return value instanceof List<?>values ? values.get(index) : value;
  }

  @Override
  public String toString() {
    return toString("");
  }

  private String toString(String indent) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < data.length; i++) {
      for (Object value : getValues(i)) {
        builder.append(indent).append(parquetSchema.getFieldName(i));
        if (value instanceof GeoParquetGroup group) {
          builder.append("\n").append(group.toString(indent + "  "));
        } else {
          builder.append(": ").append(value).append("\n");
        }
      }
    }
    return builder.toString();
  }
}
