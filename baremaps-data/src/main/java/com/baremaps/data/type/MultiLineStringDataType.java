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

package com.baremaps.data.type;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

/**
 * A {@link DataType} for reading and writing {@link MultiLineString} objects in
 * {@link ByteBuffer}s.
 */
public class MultiLineStringDataType implements DataType<MultiLineString> {

  private final LineStringDataType lineStringDataType;

  private final GeometryFactory geometryFactory;

  /**
   * Constructs a {@link MultiLineStringDataType} with a default {@link GeometryFactory}.
   */
  public MultiLineStringDataType() {
    this(new GeometryFactory());
  }

  /**
   * Constructs a {@link MultiLineStringDataType} with a specified {@link GeometryFactory}.
   *
   * @param geometryFactory the geometry factory
   */
  public MultiLineStringDataType(GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
    this.lineStringDataType = new LineStringDataType(geometryFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final MultiLineString value) {
    int size = Integer.BYTES;
    for (int i = 0; i < value.getNumGeometries(); i++) {
      size += lineStringDataType.size((LineString) value.getGeometryN(i));
    }
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final ByteBuffer buffer, final int position, final MultiLineString value) {
    buffer.putInt(position, size(value));
    var p = position + Integer.BYTES;
    for (int i = 0; i < value.getNumGeometries(); i++) {
      lineStringDataType.write(buffer, p, (LineString) value.getGeometryN(i));
      p += buffer.getInt(p);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MultiLineString read(final ByteBuffer buffer, final int position) {
    var size = size(buffer, position);
    var limit = position + size;
    var p = position + Integer.BYTES;
    var lineStrings = new ArrayList<LineString>();
    while (p < limit) {
      var lineString = lineStringDataType.read(buffer, p);
      lineStrings.add(lineString);
      p += lineStringDataType.size(buffer, p);
    }
    return geometryFactory.createMultiLineString(lineStrings.toArray(LineString[]::new));
  }
}
