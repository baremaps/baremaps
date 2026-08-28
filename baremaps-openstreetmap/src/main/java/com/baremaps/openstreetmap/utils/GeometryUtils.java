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

package com.baremaps.openstreetmap.utils;

import static org.locationtech.jts.io.WKBConstants.wkbNDR;

import com.baremaps.openstreetmap.function.ProjectionTransformer;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;

/** Utility methods for serializing and deserializing geometries. */
public class GeometryUtils {

  public static final GeometryFactory GEOMETRY_FACTORY_WGS84 =
      new GeometryFactory(new PrecisionModel(), 4326);

  private GeometryUtils() {
    // Prevent instantiation
  }

  /**
   * Serializes a geometry in the WKB format.
   *
   * @param geometry the geometry to serialize
   * @return the serialized geometry
   */
  public static byte[] serialize(Geometry geometry) {
    if (geometry == null) {
      return null;
    }
    WKBWriter writer = new WKBWriter(2, wkbNDR, true);
    return writer.write(geometry);
  }

  /**
   * Deserializes a geometry in the WKB format.
   *
   * @param wkb the serialized geometry
   * @return the deserialized geometry
   */
  public static Geometry deserialize(byte[] wkb) {
    if (wkb == null) {
      return null;
    }
    try {
      WKBReader reader = new WKBReader(new GeometryFactory());
      return reader.read(wkb);
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
  public static CoordinateTransform coordinateTransform(Integer sourceSrid, Integer targetSrid) {
    CoordinateReferenceSystem sourceCRS = CRSUtils.createFromSrid(sourceSrid);
    CoordinateReferenceSystem targetCRS = CRSUtils.createFromSrid(targetSrid);
    CoordinateTransformFactory coordinateTransformFactory = new CoordinateTransformFactory();
    return coordinateTransformFactory.createTransform(sourceCRS, targetCRS);
  }

  /**
   * Creates a projection transformer with the provided SRIDs.
   *
   * @param inputSRID the input SRID
   * @param outputSRID the output SRID
   * @return the projection transformer
   */
  public static ProjectionTransformer projectionTransformer(int inputSRID, int outputSRID) {
    return new ProjectionTransformer(inputSRID, outputSRID);
  }

  /**
   * Scales a geometry by a factor.
   *
   * @param geometry The geometry to scale
   * @param factor The factor to scale by
   * @return The scaled geometry
   */
  public static Geometry scale(Geometry geometry, double factor) {
    AffineTransformation transform = AffineTransformation.scaleInstance(factor, factor);
    return transform.transform(geometry);
  }

  /**
   * Creates an envelope with the provided bounds.
   *
   * @param xMin the minimum x coordinate
   * @param yMin the minimum y coordinate
   * @param xMax the maximum x coordinate
   * @param yMax the maximum y coordinate
   * @return the envelope geometry
   */
  public static Geometry createEnvelope(int xMin, int yMin, int xMax, int yMax) {
    return new GeometryFactory().createPolygon(new Coordinate[] {
        new Coordinate(xMin, yMin),
        new Coordinate(xMin, yMax),
        new Coordinate(xMax, yMax),
        new Coordinate(xMax, yMin),
        new Coordinate(xMin, yMin)
    });
  }
}
