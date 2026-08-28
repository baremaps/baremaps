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

package com.baremaps.dem;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.util.GeometryTransformer;

/**
 * A geometry transformer that applies the Chaikin smoothing algorithm to a geometry's coordinates.
 * This class extends the {@link GeometryTransformer} to provide a smoother version of the input
 * geometry by iteratively applying the Chaikin algorithm, which reduces sharp corners and refines
 * the shape of geometrical features.
 */
public class ChaikinSmoother extends GeometryTransformer {

  private final int iterations;

  private final double factor;

  /**
   * Constructs a {@code ChaikinSmoother} with the specified number of iterations and factor.
   *
   * @param iterations the number of iterations
   * @param factor the smoothing factor
   */
  public ChaikinSmoother(int iterations, double factor) {
    this.iterations = iterations;
    this.factor = factor;
  }

  /**
   * Transforms the coordinates of a geometry using the Chaikin smoothing algorithm.
   *
   * @param coordinateSequence the coordinates to transform
   * @param parent the parent geometry
   * @return the transformed coordinates
   */
  @Override
  protected CoordinateSequence transformCoordinates(
      CoordinateSequence coordinateSequence,
      Geometry parent) {
    return smooth(coordinateSequence, iterations, factor);
  }

  /**
   * Smooths the coordinates of a coordinate sequence using the Chaikin algorithm. This method
   * distinguishes between rings and lines and applies the algorithm accordingly.
   *
   * @param coordinateSequence the coordinates to smooth
   * @param iterations the number of iterations
   * @param factor the smoothing factor
   * @return the smoothed coordinates
   */
  public static CoordinateSequence smooth(
      CoordinateSequence coordinateSequence,
      int iterations,
      double factor) {
    if (CoordinateSequences.isRing(coordinateSequence)) {
      return new CoordinateArraySequence(chaikin(coordinateSequence.toCoordinateArray(), 2, 0.25));
    } else {
      Coordinate[] original = coordinateSequence.toCoordinateArray();
      Coordinate[] smoothed = chaikin(original, iterations, factor);
      int sumOfSquares = (iterations * (iterations + 1) * (2 * iterations + 1)) / 6;
      int trimmedLength = smoothed.length - sumOfSquares;
      Coordinate[] result = new Coordinate[trimmedLength + 2];
      result[0] = original[0];
      System.arraycopy(smoothed, 0, result, 1, trimmedLength);
      result[trimmedLength + 1] = original[original.length - 1];
      return new CoordinateArraySequence(result);
    }
  }

  /**
   * Applies the Chaikin smoothing algorithm to the specified coordinates.
   *
   * @param coordinates the coordinates to smooth
   * @param iterations the number of iterations
   * @param factor the smoothing factor
   * @return the smoothed coordinates
   */
  private static Coordinate[] chaikin(
      Coordinate[] coordinates,
      int iterations,
      double factor) {
    if (iterations <= 0) {
      return coordinates;
    }

    for (int i = 0; i < iterations; i++) {
      int l = coordinates.length;
      double f1 = 1 - factor;
      double f2 = factor;

      Coordinate[] smoothed = new Coordinate[l * 2];
      for (int j = 0; j < l; j++) {
        Coordinate c1 = coordinates[j];
        Coordinate c2 = coordinates[(j + 1) % l];
        smoothed[j * 2] = new Coordinate(
            f1 * c1.getX() + f2 * c2.getX(),
            f1 * c1.getY() + f2 * c2.getY());
        smoothed[j * 2 + 1] = new Coordinate(
            f2 * c1.getX() + f1 * c2.getX(),
            f2 * c1.getY() + f1 * c2.getY());
      }

      coordinates = smoothed;
    }

    return coordinates;
  }

}
