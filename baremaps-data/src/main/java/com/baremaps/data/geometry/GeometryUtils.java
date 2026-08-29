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

package com.baremaps.data.geometry;

import static org.locationtech.jts.io.WKBConstants.wkbNDR;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;

/** Utility methods for serializing, deserializing and reprojecting geometries. */
public class GeometryUtils {

  /** The SRID of the geographic coordinates used by OpenStreetMap and most exchange formats. */
  public static final int WGS84 = 4326;

  public static final GeometryFactory GEOMETRY_FACTORY_WGS84 =
      new GeometryFactory(new PrecisionModel(), WGS84);

  private GeometryUtils() {
    // Prevent instantiation
  }

  /**
   * Serializes a geometry in the WKB format, SRID included.
   *
   * @param geometry the geometry to serialize
   * @return the serialized geometry, or null if the geometry is null
   */
  public static byte[] serialize(Geometry geometry) {
    if (geometry == null) {
      return null;
    }
    // WKBWriter holds parsing state, so it cannot be shared between the threads of a parallel
    // import; it is cheap enough to create per call.
    return new WKBWriter(2, wkbNDR, true).write(geometry);
  }

  /**
   * Deserializes a geometry in the WKB format. The SRID is read back from the WKB itself, so the
   * factory the reader is given does not need to carry one.
   *
   * @param wkb the serialized geometry
   * @return the deserialized geometry, or null if the input is null
   */
  public static Geometry deserialize(byte[] wkb) {
    if (wkb == null) {
      return null;
    }
    try {
      // WKBReader holds parsing state; see serialize.
      return new WKBReader(new GeometryFactory()).read(wkb);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Creates a coordinate transform with the provided SRIDs.
   *
   * @param sourceSrid the source SRID
   * @param targetSrid the target SRID
   * @return the coordinate transform
   */
  public static CoordinateTransform coordinateTransform(int sourceSrid, int targetSrid) {
    return new CoordinateTransformFactory().createTransform(
        CRSUtils.createFromSrid(sourceSrid),
        CRSUtils.createFromSrid(targetSrid));
  }
}
