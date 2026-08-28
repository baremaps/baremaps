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

import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.Type;

/**
 * A {@link GroupConverter} for {@link GeoParquetGroup}s.
 */
class GeoParquetGroupConverter extends GroupConverter {

  private final GeoParquetGroupConverter parent;
  private final int index;
  protected GeoParquetGroup current;
  private final Converter[] converters;

  /**
   * Constructs a new {@code GeoParquetGroupConverter} with the specified parent, index and schema.
   *
   * @param parent the parent
   * @param index the index
   * @param schema the schema
   */
  GeoParquetGroupConverter(
      GeoParquetGroupConverter parent,
      int index,
      GroupType schema) {
    this.parent = parent;
    this.index = index;

    converters = new Converter[schema.getFieldCount()];

    for (int i = 0; i < converters.length; i++) {
      final Type type = schema.getType(i);
      if (type.isPrimitive()) {
        converters[i] = new GeoParquetPrimitiveConverter(this, i);
      } else {
        converters[i] = new GeoParquetGroupConverter(this, i, type.asGroupType());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    current = parent.getCurrentRecord().addGroup(index);
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
    current = null;
  }

  /**
   * Returns the current record.
   *
   * @return the current record
   */
  public GeoParquetGroup getCurrentRecord() {
    return current;
  }
}
