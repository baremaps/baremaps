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

import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

/**
 * A {@link RecordMaterializer} for {@link GeoParquetGroup}s.
 */
class GeoParquetGroupRecordMaterializer extends RecordMaterializer<GeoParquetGroup> {

  private final GeoParquetGroupConverter root;

  /**
   * Constructs a new {@code GeoParquetGroupRecordMaterializer} with the specified schema and
   * metadata.
   *
   * @param schema the Parquet schema of the file
   * @param metadata the GeoParquet metadata of the file, which may be null
   */
  GeoParquetGroupRecordMaterializer(MessageType schema, GeoParquetMetadata metadata) {
    // The GeoParquet schema is derived once and shared by every record of the file.
    GeoParquetSchema geoParquetSchema = GeoParquetSchema.of(schema, metadata);
    this.root =
        new GeoParquetGroupConverter(() -> new GeoParquetGroup(schema, geoParquetSchema), schema);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GeoParquetGroup getCurrentRecord() {
    return root.getCurrentRecord();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public GroupConverter getRootConverter() {
    return root;
  }
}
