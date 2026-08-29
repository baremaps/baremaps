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

import java.util.function.Supplier;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;

/**
 * A {@link GroupConverter} that assembles a {@link GeoParquetGroup}.
 * <p>
 * Where a group comes from differs between the root, which starts a fresh record, and a nested
 * group, which is appended to its parent. The difference is expressed as the supplier the converter
 * is built with, so that the conversion itself has no root to special case.
 */
class GeoParquetGroupConverter extends GroupConverter {

  private final Supplier<GeoParquetGroup> groupSupplier;
  private final Converter[] converters;

  private GeoParquetGroup current;

  /**
   * Constructs a new {@code GeoParquetGroupConverter}.
   *
   * @param groupSupplier supplies the group the fields are written to, once per occurrence
   * @param schema the Parquet type of the group
   */
  GeoParquetGroupConverter(Supplier<GeoParquetGroup> groupSupplier, GroupType schema) {
    this.groupSupplier = groupSupplier;
    this.converters = new Converter[schema.getFieldCount()];
    for (int i = 0; i < converters.length; i++) {
      Type type = schema.getType(i);
      int fieldIndex = i;
      converters[i] = type.isPrimitive()
          ? new GeoParquetPrimitiveConverter(this, fieldIndex)
          : new GeoParquetGroupConverter(() -> current.addGroup(fieldIndex), type.asGroupType());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    current = groupSupplier.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Converter getConverter(int fieldIndex) {
    return converters[fieldIndex];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() {
    // The group is kept until the next occurrence starts, so that the record materializer can read
    // the record the root converter just assembled.
  }

  /**
   * Returns the group being assembled.
   *
   * @return the current group
   */
  GeoParquetGroup getCurrentRecord() {
    return current;
  }
}
