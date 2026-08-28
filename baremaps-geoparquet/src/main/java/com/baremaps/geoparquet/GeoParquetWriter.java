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
import org.apache.hadoop.fs.Path;
import org.apache.parquet.conf.ParquetConfiguration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.schema.MessageType;

/**
 * A writer for GeoParquet files that writes GeoParquetGroup instances to a Parquet file.
 */
public class GeoParquetWriter {

  private GeoParquetWriter() {
    // Prevent instantiation
  }

  public static Builder builder(Path file) {
    return new Builder(file);
  }

  public static class Builder
      extends ParquetWriter.Builder<GeoParquetGroup, GeoParquetWriter.Builder> {

    private MessageType type = null;

    private GeoParquetMetadata metadata = null;

    private Builder(Path file) {
      super(file);
    }

    /**
     * Replace the message type with the specified one.
     *
     * @param type the message type
     * @return the builder
     */
    public GeoParquetWriter.Builder withType(MessageType type) {
      this.type = type;
      return this;
    }

    /**
     * Replace the metadata with the specified one.
     *
     * @param metadata the metadata
     * @return the builder
     */
    public GeoParquetWriter.Builder withGeoParquetMetadata(GeoParquetMetadata metadata) {
      this.metadata = metadata;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WriteSupport<GeoParquetGroup> getWriteSupport(Configuration conf) {
      // We don't need access to the hadoop configuration for now
      return getWriteSupport((ParquetConfiguration) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WriteSupport<GeoParquetGroup> getWriteSupport(ParquetConfiguration conf) {
      return new GeoParquetWriteSupport(type, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GeoParquetWriter.Builder self() {
      return this;
    }
  }
}
