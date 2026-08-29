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
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * A {@link DataType} for any JTS {@link Geometry}: a tag byte identifying the geometry type,
 * followed by the encoding of the type-specific data type.
 */
public class GeometryDataType implements DataType<Geometry> {

  private final DataType<Geometry>[] types;

  public GeometryDataType() {
    this(new GeometryFactory());
  }

  @SuppressWarnings("unchecked")
  public GeometryDataType(GeometryFactory geometryFactory) {
    // Indexed by tag; tag 0 is left unused so that zero-filled memory is never a valid geometry.
    this.types = new DataType[] {
        null,
        new PointDataType(geometryFactory),
        new LineStringDataType(geometryFactory),
        new PolygonDataType(geometryFactory),
        new MultiPointDataType(geometryFactory),
        new MultiLineStringDataType(geometryFactory),
        new MultiPolygonDataType(geometryFactory),
        new GeometryCollectionDataType(geometryFactory, this),
    };
  }

  // The order matters: LinearRing extends LineString and the Multi* types extend
  // GeometryCollection.
  private static int tagOf(Geometry value) {
    Objects.requireNonNull(value, "Geometry cannot be null");
    if (value instanceof Point) {
      return 1;
    } else if (value instanceof LineString) {
      return 2;
    } else if (value instanceof Polygon) {
      return 3;
    } else if (value instanceof MultiPoint) {
      return 4;
    } else if (value instanceof MultiLineString) {
      return 5;
    } else if (value instanceof MultiPolygon) {
      return 6;
    } else if (value instanceof GeometryCollection) {
      return 7;
    }
    throw new IllegalArgumentException("Unsupported geometry type: " + value.getClass());
  }

  private DataType<Geometry> typeOf(int tag) {
    if (tag <= 0 || tag >= types.length) {
      throw new IllegalArgumentException("Unsupported geometry tag: " + tag);
    }
    return types[tag];
  }

  @Override
  public int size(final Geometry value) {
    return Byte.BYTES + typeOf(tagOf(value)).size(value);
  }

  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return Byte.BYTES + typeOf(buffer.get(position)).size(buffer, position + Byte.BYTES);
  }

  @Override
  public void write(final ByteBuffer buffer, final int position, final Geometry value) {
    int tag = tagOf(value);
    buffer.put(position, (byte) tag);
    typeOf(tag).write(buffer, position + Byte.BYTES, value);
  }

  @Override
  public Geometry read(final ByteBuffer buffer, final int position) {
    return typeOf(buffer.get(position)).read(buffer, position + Byte.BYTES);
  }
}
