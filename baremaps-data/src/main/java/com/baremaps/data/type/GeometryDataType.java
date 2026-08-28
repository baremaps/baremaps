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
import org.locationtech.jts.geom.*;

/**
 * A {@link DataType} for reading and writing {@link Geometry} objects in {@link ByteBuffer}s.
 */
public class GeometryDataType implements DataType<Geometry> {

  public static final String UNSUPPORTED_GEOMETRY = "Unsupported geometry type: ";
  private final PointDataType pointDataType;

  private final LineStringDataType lineStringDataType;

  private final PolygonDataType polygonDataType;

  private final MultiPointDataType multiPointDataType;

  private final MultiLineStringDataType multiLineStringDataType;

  private final MultiPolygonDataType multiPolygonDataType;

  private final GeometryCollectionDataType geometryCollectionDataType;

  /**
   * Constructs a {@link GeometryDataType} with a default {@link GeometryFactory}.
   */
  public GeometryDataType() {
    this(new GeometryFactory());
  }

  /**
   * Constructs a {@link GeometryDataType} with a specified {@link GeometryFactory}.
   *
   * @param geometryFactory the geometry factory
   */
  public GeometryDataType(GeometryFactory geometryFactory) {
    this.pointDataType = new PointDataType(geometryFactory);
    this.lineStringDataType = new LineStringDataType(geometryFactory);
    this.polygonDataType = new PolygonDataType(geometryFactory);
    this.multiPointDataType = new MultiPointDataType(geometryFactory);
    this.multiLineStringDataType = new MultiLineStringDataType(geometryFactory);
    this.multiPolygonDataType = new MultiPolygonDataType(geometryFactory);
    this.geometryCollectionDataType = new GeometryCollectionDataType(geometryFactory, this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(Geometry value) {
    var size = 0;

    // Geometry type
    size += Byte.BYTES;

    // Size of the geometry
    if (value instanceof Point point) {
      size += pointDataType.size(point);
    } else if (value instanceof LineString lineString) {
      size += lineStringDataType.size(lineString);
    } else if (value instanceof Polygon polygon) {
      size += polygonDataType.size(polygon);
    } else if (value instanceof MultiPoint multiPoint) {
      size += multiPointDataType.size(multiPoint);
    } else if (value instanceof MultiLineString multiLineString) {
      size += multiLineStringDataType.size(multiLineString);
    } else if (value instanceof MultiPolygon multiPolygon) {
      size += multiPolygonDataType.size(multiPolygon);
    } else if (value instanceof GeometryCollection geometryCollection) {
      size += geometryCollectionDataType.size(geometryCollection);
    } else {
      throw new IllegalArgumentException(UNSUPPORTED_GEOMETRY + value.getClass());
    }

    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size(ByteBuffer buffer, int position) {
    var size = 0;

    // Geometry type
    var type = buffer.get(position);
    size += Byte.BYTES;

    // Size of the geometry
    if (type == 1) {
      size += pointDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 2) {
      size += lineStringDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 3) {
      size += polygonDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 4) {
      size += multiPointDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 5) {
      size += multiLineStringDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 6) {
      size += multiPolygonDataType.size(buffer, position + Byte.BYTES);
    } else if (type == 7) {
      size += geometryCollectionDataType.size(buffer, position + Byte.BYTES);
    } else {
      throw new IllegalArgumentException(UNSUPPORTED_GEOMETRY + type);
    }

    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Geometry value) {

    // Write the geometry
    if (value == null) {
      buffer.put(position, (byte) 0);
    } else if (value instanceof Point point) {
      buffer.put(position, (byte) 1);
      pointDataType.write(buffer, position + Byte.BYTES, point);
    } else if (value instanceof LineString lineString) {
      buffer.put(position, (byte) 2);
      lineStringDataType.write(buffer, position + Byte.BYTES, lineString);
    } else if (value instanceof Polygon polygon) {
      buffer.put(position, (byte) 3);
      polygonDataType.write(buffer, position + Byte.BYTES, polygon);
    } else if (value instanceof MultiPoint multiPoint) {
      buffer.put(position, (byte) 4);
      multiPointDataType.write(buffer, position + Byte.BYTES, multiPoint);
    } else if (value instanceof MultiLineString multiLineString) {
      buffer.put(position, (byte) 5);
      multiLineStringDataType.write(buffer, position + Byte.BYTES, multiLineString);
    } else if (value instanceof MultiPolygon multiPolygon) {
      buffer.put(position, (byte) 6);
      multiPolygonDataType.write(buffer, position + Byte.BYTES, multiPolygon);
    } else if (value instanceof GeometryCollection geometryCollection) {
      buffer.put(position, (byte) 7);
      geometryCollectionDataType.write(buffer, position + Byte.BYTES, geometryCollection);
    } else {
      throw new IllegalArgumentException(UNSUPPORTED_GEOMETRY + value.getClass());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Geometry read(final ByteBuffer buffer, final int position) {
    var p = position;

    // Read the geometry type
    var type = buffer.get(p);
    p += Byte.BYTES;

    // Read the geometry
    if (type == 0) {
      return null;
    } else if (type == 1) {
      return pointDataType.read(buffer, p);
    } else if (type == 2) {
      return lineStringDataType.read(buffer, p);
    } else if (type == 3) {
      return polygonDataType.read(buffer, p);
    } else if (type == 4) {
      return multiPointDataType.read(buffer, p);
    } else if (type == 5) {
      return multiLineStringDataType.read(buffer, p);
    } else if (type == 6) {
      return multiPolygonDataType.read(buffer, p);
    } else if (type == 7) {
      return geometryCollectionDataType.read(buffer, p);
    } else {
      throw new IllegalArgumentException(UNSUPPORTED_GEOMETRY + type);
    }
  }
}
