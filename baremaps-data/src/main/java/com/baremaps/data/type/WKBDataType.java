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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;
import static org.locationtech.jts.io.WKBConstants.wkbNDR;

import java.lang.foreign.MemorySegment;
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

/**
 * A {@link DataType} for reading and writing {@link Geometry} objects in Well-Known Binary (WKB)
 * format in {@link MemorySegment}s.
 */
public class WKBDataType implements DataType<Geometry> {

  /** {@inheritDoc} */
  @Override
  public int size(final Geometry value) {
    return Integer.BYTES + serialize(value).length;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final Geometry value) {
    byte[] bytes = serialize(value);
    segment.set(JAVA_INT_UNALIGNED, position, Integer.BYTES + bytes.length);
    MemorySegment.copy(bytes, 0, segment, JAVA_BYTE, position + Integer.BYTES, bytes.length);
  }

  /** {@inheritDoc} */
  @Override
  public Geometry read(final MemorySegment segment, final long position) {
    int size = segment.get(JAVA_INT_UNALIGNED, position);
    byte[] bytes = new byte[size - Integer.BYTES];
    MemorySegment.copy(segment, JAVA_BYTE, position + Integer.BYTES, bytes, 0, bytes.length);
    return deserialize(bytes);
  }

  /**
   * Serializes a geometry into the WKB format.
   *
   * @param geometry the geometry to serialize
   * @return the serialized geometry as a byte array, or null if the input is null
   */
  private static byte[] serialize(Geometry geometry) {
    Objects.requireNonNull(geometry, "Geometry cannot be null");
    WKBWriter writer = new WKBWriter(2, wkbNDR, true);
    return writer.write(geometry);
  }

  /**
   * Deserializes a geometry from the WKB format.
   *
   * @param wkb the WKB byte array to deserialize
   * @return the deserialized geometry, or null if the input is null
   * @throws IllegalArgumentException if the WKB cannot be parsed
   */
  private static Geometry deserialize(byte[] wkb) {
    try {
      WKBReader reader = new WKBReader(new GeometryFactory());
      return reader.read(wkb);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
