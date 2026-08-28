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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.*;

/**
 * WriteSupport implementation for writing GeoParquetGroup instances to Parquet.
 */
public class GeoParquetWriteSupport extends WriteSupport<GeoParquetGroup> {

  private Configuration configuration;
  private final MessageType schema;
  private final GeoParquetMetadata metadata;
  private RecordConsumer recordConsumer;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Constructs a new GeoParquetWriteSupport.
   *
   * @param schema the Parquet schema
   * @param metadata the GeoParquet metadata
   */
  public GeoParquetWriteSupport(MessageType schema, GeoParquetMetadata metadata) {
    this.schema = schema;
    this.metadata = metadata;
  }

  @Override
  public WriteContext init(Configuration configuration) {
    Map<String, String> extraMetadata = new HashMap<>();
    String geoMetadataJson = serializeMetadata(metadata);
    extraMetadata.put("geo", geoMetadataJson);
    return new WriteContext(schema, extraMetadata);
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(GeoParquetGroup group) {
    recordConsumer.startMessage();
    writeGroup(group, schema, true);
    recordConsumer.endMessage();
  }

  private void writeGroup(GeoParquetGroup group, GroupType groupType, boolean isRoot) {
    if (!isRoot) {
      recordConsumer.startGroup();
    }
    for (int i = 0; i < groupType.getFieldCount(); i++) {
      Type fieldType = groupType.getType(i);
      String fieldName = fieldType.getName();
      int repetitionCount = group.getFieldRepetitionCount(i);
      if (repetitionCount == 0) {
        continue; // Skip if no values are present
      }
      for (int j = 0; j < repetitionCount; j++) {
        recordConsumer.startField(fieldName, i);
        if (fieldType.isPrimitive()) {
          Object value = group.getValue(i, j);
          writePrimitive(value, fieldType.asPrimitiveType());
        } else {
          GeoParquetGroup childGroup = group.getGroup(i, j);
          writeGroup(childGroup, fieldType.asGroupType(), false);
        }
        recordConsumer.endField(fieldName, i);
      }
    }
    if (!isRoot) {
      recordConsumer.endGroup();
    }
  }

  private void writePrimitive(Object value, PrimitiveType primitiveType) {
    if (value == null) {
      // The Parquet format does not support writing null values directly.
      // If the field is optional and the value is null, we simply do not write it.
      return;
    }
    switch (primitiveType.getPrimitiveTypeName()) {
      case INT32:
        recordConsumer.addInteger((Integer) value);
        break;
      case INT64:
        recordConsumer.addLong((Long) value);
        break;
      case FLOAT:
        recordConsumer.addFloat((Float) value);
        break;
      case DOUBLE:
        recordConsumer.addDouble((Double) value);
        break;
      case BOOLEAN:
        recordConsumer.addBoolean((Boolean) value);
        break;
      case BINARY, FIXED_LEN_BYTE_ARRAY:
        recordConsumer.addBinary((Binary) value);
        break;
      default:
        throw new GeoParquetException(
            "Unsupported type: " + primitiveType.getPrimitiveTypeName());
    }
  }

  private String serializeMetadata(GeoParquetMetadata metadata) {
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JsonProcessingException e) {
      throw new GeoParquetException("Failed to serialize GeoParquet metadata", e);
    }
  }
}
