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

/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

/**
 * A {@link RecordMaterializer} for {@link GeoParquetGroup}s.
 */
class GeoParquetGroupRecordMaterializer extends RecordMaterializer<GeoParquetGroup> {

  private final GeoParquetGroupFactory groupFactory;

  private final GeoParquetGroupConverter root;

  /**
   * Constructs a new {@code GeoParquetGroupRecordMaterializer} with the specified schema and
   * metadata.
   *
   * @param schema the schema
   * @param metadata the metadata
   */
  public GeoParquetGroupRecordMaterializer(MessageType schema, GeoParquetMetadata metadata) {
    this.groupFactory = new GeoParquetGroupFactory(schema, metadata);
    this.root = new GeoParquetGroupConverter(null, 0, schema) {
      @Override
      public void start() {
        this.current = groupFactory.newGroup();
      }

      @Override
      public void end() {
        // Do nothing
      }
    };
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
