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

package com.baremaps.maplibre.vectortile;

import java.nio.ByteBuffer;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * Utility class for vector tiles.
 */
public class VectorTileFunctions {

  public static final int MOVE_TO = 1;

  public static final int LINE_TO = 2;

  public static final int CLOSE_PATH = 7;

  private VectorTileFunctions() {
    // Prevent instantiation
  }

  /**
   * Transforms a geometry into a vector tile geometry.
   *
   * @param geometry The geometry to transform
   * @param envelope The envelope of the tile
   * @param extent The extent of the tile
   * @param buffer The buffer of the tile
   * @param clipGeom A flag to clip the geometry
   * @return The transformed geometry
   */
  public static Geometry asVectorTileGeom(Geometry geometry, Geometry envelope, int extent,
      int buffer, boolean clipGeom) {
    // Scale the geometry to the extent of the tile
    var envelopeInternal = envelope.getEnvelopeInternal();
    double scaleX = extent / envelopeInternal.getWidth();
    double scaleY = extent / envelopeInternal.getHeight();
    AffineTransformation affineTransformation = new AffineTransformation();
    affineTransformation.translate(-envelopeInternal.getMinX(), -envelopeInternal.getMinY());
    affineTransformation.scale(scaleX, -scaleY);
    affineTransformation.translate(0, extent);
    Geometry scaledGeometry = affineTransformation.transform(geometry);

    // Build the final geometry
    if (clipGeom) {
      return clipToTile(scaledGeometry, extent, buffer);
    } else {
      return scaledGeometry;
    }
  }

  /**
   * Transforms a geometry into a vector tile geometry.
   *
   * @param geometry The geometry to transform
   * @param envelope The envelope of the tile
   * @param extent The extent of the tile
   * @return The transformed geometry
   */
  public static Geometry fromVectorTileGeom(Geometry geometry, Geometry envelope, int extent) {
    // Scale the geometry to the extent of the tile
    var envelopeInternal = envelope.getEnvelopeInternal();
    double scaleX = extent / envelopeInternal.getWidth();
    double scaleY = extent / envelopeInternal.getHeight();
    AffineTransformation affineTransformation = new AffineTransformation();
    affineTransformation.translate(0, -extent);
    affineTransformation.scale(1 / scaleX, -1 / scaleY);
    affineTransformation.translate(envelopeInternal.getMinX(), envelopeInternal.getMinY());

    // Build the final geometry
    return affineTransformation.transform(geometry);
  }

  /**
   * Transforms a tile into a vector tile.
   *
   * @param vectorTile The tile to transform
   * @return The transformed tile
   */
  public static ByteBuffer asVectorTile(Tile vectorTile) {
    return new VectorTileEncoder()
        .encodeTile(vectorTile)
        .toByteString()
        .asReadOnlyByteBuffer();

  }

  /**
   * Transforms a layer into a vector tile layer.
   *
   * @param layer The layer to transform
   * @return The transformed layer
   */
  public static ByteBuffer asVectorTileLayer(Layer layer) {
    return new VectorTileEncoder()
        .encodeLayer(layer)
        .toByteString()
        .asReadOnlyByteBuffer();
  }

  /**
   * Clips a geometry to a tile.
   *
   * @param geometry The geometry to clip
   * @param extent The extent of the tile
   * @param buffer The buffer of the tile
   * @return The clipped geometry
   */
  private static Geometry clipToTile(Geometry geometry, int extent, int buffer) {
    Envelope envelope = new Envelope(
        -buffer, (double) extent + buffer,
        -buffer, (double) extent + buffer);
    GeometryFactory geometryFactory = new GeometryFactory();
    Geometry tile = geometryFactory.toGeometry(envelope);
    return geometry.intersection(tile);
  }


  /**
   * Returns true if the winding order of the vector tile geometry is clockwise.
   *
   * @param geometry The vector tile geometry
   * @return True if the winding order is clockwise
   */
  public static boolean isClockWise(Geometry geometry) {
    return isClockWise(geometry.getCoordinates());
  }

  /**
   * Returns true if the winding order of a ring is clockwise, by the sign of the area the
   * specification measures it with.
   *
   * <p>
   * The specification tells a polygon's outline from its holes by the sign of the surveyor's
   * formula over the ring, and that is what this computes. It is not the same question as which way
   * a ring is wound geometrically, which is what {@link Orientation#isCCW} answers: that answer is
   * only defined for a ring that does not touch itself, and rounding a traced contour onto the tile
   * grid regularly pinches one against itself, at which point the two disagree. Since the decoder
   * has nothing but these coordinates to judge by, the encoder has to judge by the same thing, or
   * it writes an outline that is read back as a hole.
   *
   * @param coordinates the coordinates of the ring, closed
   * @return true if the winding order is clockwise
   */
  public static boolean isClockWise(Coordinate[] coordinates) {
    // As the origin of the vector tile coordinate system is in the top left corner, a ring that is
    // counter-clockwise by the formula is clockwise on the screen.
    double area = 0;
    for (int i = 0; i < coordinates.length - 1; i++) {
      area += coordinates[i].getX() * coordinates[i + 1].getY()
          - coordinates[i + 1].getX() * coordinates[i].getY();
    }
    return area > 0;
  }
}
