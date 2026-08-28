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

import com.baremaps.geoparquet.GeoParquetSchema.EnvelopeField;
import com.baremaps.geoparquet.GeoParquetSchema.Field;
import com.baremaps.geoparquet.GeoParquetSchema.GroupField;
import com.baremaps.geoparquet.GeoParquetSchema.Type;
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
 */
public class GeoParquetGroup {

  private final GroupType parquetSchema;
  private final GeoParquetMetadata geoParquetMetadata;
  private final GeoParquetSchema geoParquetSchema;
  private final Object[] data;

  /**
   * Constructs a new GeoParquetGroup with the specified schema, metadata and GeoParquet schema.
   *
   * @param parquetSchema
   * @param geoParquetMetadata
   * @param geoParquetSchema
   */
  public GeoParquetGroup(GroupType parquetSchema, GeoParquetMetadata geoParquetMetadata,
      GeoParquetSchema geoParquetSchema) {
    this.parquetSchema = parquetSchema;
    this.geoParquetMetadata = geoParquetMetadata;
    this.geoParquetSchema = geoParquetSchema;
    this.data = new Object[parquetSchema.getFields().size()];
    for (int i = 0; i < parquetSchema.getFieldCount(); i++) {
      Field field = geoParquetSchema.fields().get(i);
      if (field.cardinality() == GeoParquetSchema.Cardinality.REPEATED) {
        this.data[i] = new ArrayList<>();
      } else {
        this.data[i] = null; // For REQUIRED or OPTIONAL fields
      }
    }
  }

  public GeoParquetGroup addGroup(int fieldIndex) {
    GeoParquetGroup group = createGroup(fieldIndex);
    add(fieldIndex, group);
    return group;
  }

  public GeoParquetGroup addGroup(String field) {
    return addGroup(getParquetSchema().getFieldIndex(field));
  }

  private GeoParquetGroup createGroup(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    if (field instanceof EnvelopeField envelopeField) {
      return new GeoParquetGroup(parquetSchema.getType(fieldIndex).asGroupType(),
          geoParquetMetadata,
          envelopeField.schema());
    } else if (field instanceof GroupField groupField) {
      return new GeoParquetGroup(parquetSchema.getType(fieldIndex).asGroupType(),
          geoParquetMetadata,
          groupField.schema());
    }
    throw new GeoParquetException("Field at index " + fieldIndex + " is not a group");
  }

  public GeoParquetGroup getGroup(int fieldIndex, int index) {
    return (GeoParquetGroup) getValue(fieldIndex, index);
  }

  public GeoParquetGroup getGroup(String field, int index) {
    return getGroup(getParquetSchema().getFieldIndex(field), index);
  }

  public int getFieldRepetitionCount(int fieldIndex) {
    Object value = data[fieldIndex];
    if (value instanceof List<?>list) {
      return list.size();
    } else {
      return value == null ? 0 : 1;
    }
  }

  Object getValue(int fieldIndex, int index) {
    Object value = data[fieldIndex];
    if (value instanceof List<?>list) {
      return list.get(index);
    } else if (index == 0) {
      return value;
    } else {
      throw createGeoParquetException(fieldIndex, "element number " + index);
    }
  }

  private Object getValue(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    if (field.cardinality() == GeoParquetSchema.Cardinality.REPEATED) {
      throw new IllegalStateException("Field " + fieldIndex + " (" + field.name()
          + ") is repeated. Use getValues() instead.");
    }
    return data[fieldIndex];
  }

  public List<Object> getValues(int fieldIndex) {
    Field field = geoParquetSchema.fields().get(fieldIndex);
    if (field.cardinality() != GeoParquetSchema.Cardinality.REPEATED) {
      return List.of(getValue(fieldIndex));
    }
    return (List<Object>) data[fieldIndex];
  }

  private void addValue(int fieldIndex, Object value) {
    Object currentValue = data[fieldIndex];
    if (currentValue instanceof List<?>) {
      ((List<Object>) currentValue).add(value);
    } else {
      data[fieldIndex] = value;
    }
  }

  public void add(int fieldIndex, int value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, long value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, String value) {
    addValue(fieldIndex, Binary.fromString(value));
  }

  public void add(int fieldIndex, boolean value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, Binary value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, float value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, double value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, GeoParquetGroup value) {
    addValue(fieldIndex, value);
  }

  public void add(int fieldIndex, Geometry geometry) {
    byte[] bytes = new WKBWriter().write(geometry);
    add(fieldIndex, Binary.fromConstantByteArray(bytes));
  }

  public void add(String field, int value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, long value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, float value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, double value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, String value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, boolean value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, Binary value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, GeoParquetGroup value) {
    add(getParquetSchema().getFieldIndex(field), value);
  }

  public void add(String field, Geometry geometry) {
    add(getParquetSchema().getFieldIndex(field), geometry);
  }

  private GeoParquetException createGeoParquetException(int fieldIndex, String elementText) {
    String msg = String.format("Not found %d (%s) %s in group%n%s", fieldIndex,
        parquetSchema.getFieldName(fieldIndex), elementText, this);
    return new GeoParquetException(msg);
  }

  public GeoParquetSchema getGeoParquetSchema() {
    return geoParquetSchema;
  }

  public GroupType getParquetSchema() {
    return parquetSchema;
  }

  public GeoParquetMetadata getGeoParquetMetadata() {
    return geoParquetMetadata;
  }

  // Getter methods for different data types
  public String getStringValue(int fieldIndex, int index) {
    return getBinaryValue(fieldIndex, index).toStringUsingUTF8();
  }

  public Binary getBinaryValue(int fieldIndex, int index) {
    return (Binary) getValue(fieldIndex, index);
  }

  public Geometry getGeometry(int fieldIndex, int index) {
    byte[] bytes = getBinaryValue(fieldIndex, index).getBytes();
    try {
      return new WKBReader().read(bytes);
    } catch (ParseException e) {
      throw new GeoParquetException("WKBReader failed to parse", e);
    }
  }

  // Simplify getter methods for single values
  public Binary getBinaryValue(int fieldIndex) {
    return (Binary) getValue(fieldIndex);
  }

  public Boolean getBooleanValue(int fieldIndex) {
    return (Boolean) getValue(fieldIndex);
  }

  public Double getDoubleValue(int fieldIndex) {
    return (Double) getValue(fieldIndex);
  }

  public Float getFloatValue(int fieldIndex) {
    return (Float) getValue(fieldIndex);
  }

  public Integer getIntegerValue(int fieldIndex) {
    return (Integer) getValue(fieldIndex);
  }

  public Long getLongValue(int fieldIndex) {
    return (Long) getValue(fieldIndex);
  }

  public String getStringValue(int fieldIndex) {
    return getStringValue(fieldIndex, 0);
  }

  public Geometry getGeometryValue(int fieldIndex) {
    return getGeometry(fieldIndex, 0);
  }

  public GeoParquetGroup getGroupValue(int fieldIndex) {
    return (GeoParquetGroup) getValue(fieldIndex);
  }

  // Simplify getter methods for list of values
  private <T> List<T> getValuesOfType(int fieldIndex, Class<T> clazz) {
    return getValues(fieldIndex).stream().map(clazz::cast).toList();
  }

  public List<Binary> getBinaryValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Binary.class);
  }

  public List<Boolean> getBooleanValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Boolean.class);
  }

  public List<Double> getDoubleValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Double.class);
  }

  public List<Float> getFloatValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Float.class);
  }

  public List<Integer> getIntegerValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Integer.class);
  }

  public List<Long> getLongValues(int fieldIndex) {
    return getValuesOfType(fieldIndex, Long.class);
  }

  public List<String> getStringValues(int fieldIndex) {
    return getValues(fieldIndex).stream()
        .map(value -> ((Binary) value).toStringUsingUTF8())
        .toList();
  }

  public List<Geometry> getGeometryValues(int fieldIndex) {
    return getValues(fieldIndex).stream()
        .map(value -> {
          try {
            return new WKBReader().read(((Binary) value).getBytes());
          } catch (ParseException e) {
            throw new GeoParquetException("WKBReader failed to parse.", e);
          }
        })
        .toList();
  }

  public List<GeoParquetGroup> getGroupValues(int fieldIndex) {
    if (data[fieldIndex] instanceof List<?>list) {
      return (List<GeoParquetGroup>) list;
    } else {
      return List.of((GeoParquetGroup) data[fieldIndex]);
    }
  }

  // Helper method to get numeric value (float or double)
  private double getNumericValue(GeoParquetGroup group, int fieldIndex) {
    Type fieldType = group.getGeoParquetSchema().fields().get(fieldIndex).type();
    return switch (fieldType) {
      case FLOAT -> group.getFloatValue(fieldIndex);
      case DOUBLE -> group.getDoubleValue(fieldIndex);
      default -> throw new GeoParquetException("Expected numeric field at index " + fieldIndex);
    };
  }

  public Envelope getEnvelopeValue(int fieldIndex) {
    return getEnvelopeValues(fieldIndex).get(0);
  }

  public List<Envelope> getEnvelopeValues(int fieldIndex) {
    return getGroupValues(fieldIndex).stream().map(group -> {
      double xMin = getNumericValue(group, 0);
      double yMin = getNumericValue(group, 1);
      double xMax = getNumericValue(group, 2);
      double yMax = getNumericValue(group, 3);
      return new Envelope(xMin, xMax, yMin, yMax);
    }).toList();
  }

  // Methods to access fields by name
  public Binary getBinaryValue(String fieldName) {
    return getBinaryValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Binary> getBinaryValues(String fieldName) {
    return getBinaryValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Boolean getBooleanValue(String fieldName) {
    return getBooleanValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Boolean> getBooleanValues(String fieldName) {
    return getBooleanValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Double getDoubleValue(String fieldName) {
    return getDoubleValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Double> getDoubleValues(String fieldName) {
    return getDoubleValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Float getFloatValue(String fieldName) {
    return getFloatValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Float> getFloatValues(String fieldName) {
    return getFloatValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Integer getIntegerValue(String fieldName) {
    return getIntegerValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Integer> getIntegerValues(String fieldName) {
    return getIntegerValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Long getLongValue(String fieldName) {
    return getLongValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Long> getLongValues(String fieldName) {
    return getLongValues(parquetSchema.getFieldIndex(fieldName));
  }

  public String getStringValue(String fieldName) {
    return getStringValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<String> getStringValues(String fieldName) {
    return getStringValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Geometry getGeometryValue(String fieldName) {
    return getGeometryValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Geometry> getGeometryValues(String fieldName) {
    return getGeometryValues(parquetSchema.getFieldIndex(fieldName));
  }

  public GeoParquetGroup getGroupValue(String fieldName) {
    return getGroupValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<GeoParquetGroup> getGroupValues(String fieldName) {
    return getGroupValues(parquetSchema.getFieldIndex(fieldName));
  }

  public Envelope getEnvelopeValue(String fieldName) {
    return getEnvelopeValue(parquetSchema.getFieldIndex(fieldName));
  }

  public List<Envelope> getEnvelopeValues(String fieldName) {
    return getEnvelopeValues(parquetSchema.getFieldIndex(fieldName));
  }

  public String toString() {
    return toString("");
  }

  private String toString(String indent) {
    StringBuilder builder = new StringBuilder();
    int fieldCount = parquetSchema.getFields().size();

    for (int i = 0; i < fieldCount; i++) {
      String fieldName = parquetSchema.getFieldName(i);
      Object fieldValue = data[i];
      if (fieldValue != null) {
        appendFieldToString(builder, indent, fieldName, fieldValue);
      }
    }

    return builder.toString();
  }

  private void appendFieldToString(StringBuilder builder, String indent, String fieldName,
      Object fieldValue) {
    if (fieldValue instanceof List<?>values) {
      for (Object value : values) {
        appendValueToString(builder, indent, fieldName, value);
      }
    } else {
      appendValueToString(builder, indent, fieldName, fieldValue);
    }
  }

  private void appendValueToString(StringBuilder builder, String indent, String fieldName,
      Object value) {
    builder.append(indent).append(fieldName);
    if (value == null) {
      builder.append(": NULL\n");
    } else if (value instanceof GeoParquetGroup group) {
      builder.append("\n").append(group.toString(indent + "  "));
    } else {
      String valueString = getValueAsString(value);
      builder.append(": ").append(valueString).append("\n");
    }
  }

  private String getValueAsString(Object value) {
    if (value instanceof Binary binary) {
      return binary.toStringUsingUTF8();
    } else {
      return value.toString();
    }
  }

}
