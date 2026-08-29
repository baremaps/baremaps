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

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

/**
 * Writes {@link GeoParquetGroup}s to a Parquet file, along with the GeoParquet metadata that makes
 * the file readable as GeoParquet.
 */
public class GeoParquetWriteSupport extends WriteSupport<GeoParquetGroup> {

  private final MessageType schema;
  private final GeoParquetMetadata metadata;

  private RecordConsumer recordConsumer;

  /**
   * Constructs a new {@code GeoParquetWriteSupport}.
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
    if (metadata == null) {
      throw new GeoParquetException("Writing a GeoParquet file requires GeoParquet metadata.");
    }
    return new WriteContext(schema, metadata.write());
  }

  @Override
  public void prepareForWrite(RecordConsumer recordConsumer) {
    this.recordConsumer = recordConsumer;
  }

  @Override
  public void write(GeoParquetGroup group) {
    recordConsumer.startMessage();
    writeFields(group, schema);
    recordConsumer.endMessage();
  }

  private void writeFields(GeoParquetGroup group, GroupType groupType) {
    for (int i = 0; i < groupType.getFieldCount(); i++) {
      int repetitionCount = group.getFieldRepetitionCount(i);
      if (repetitionCount == 0) {
        // Absent values are not written at all: Parquet records their absence in the definition
        // levels rather than as a value.
        continue;
      }
      Type fieldType = groupType.getType(i);
      String fieldName = fieldType.getName();
      // A field is announced once, however many values it holds. Announcing it once per value
      // would restart the repetition levels and corrupt repeated fields.
      recordConsumer.startField(fieldName, i);
      for (int j = 0; j < repetitionCount; j++) {
        Object value = group.getRawValue(i, j);
        if (fieldType.isPrimitive()) {
          writePrimitive(value, fieldType.asPrimitiveType());
        } else {
          recordConsumer.startGroup();
          writeFields((GeoParquetGroup) value, fieldType.asGroupType());
          recordConsumer.endGroup();
        }
      }
      recordConsumer.endField(fieldName, i);
    }
  }

  private void writePrimitive(Object value, PrimitiveType primitiveType) {
    switch (primitiveType.getPrimitiveTypeName()) {
      case INT32 -> recordConsumer.addInteger((Integer) value);
      case INT64 -> recordConsumer.addLong((Long) value);
      case FLOAT -> recordConsumer.addFloat((Float) value);
      case DOUBLE -> recordConsumer.addDouble((Double) value);
      case BOOLEAN -> recordConsumer.addBoolean((Boolean) value);
      case INT96, BINARY, FIXED_LEN_BYTE_ARRAY -> recordConsumer.addBinary((Binary) value);
    }
  }
}
