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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.util.GeometryTransformer;

/**
 * Rounds the corners of the geometries it transforms with Chaikin's algorithm: every pass replaces
 * each vertex by two points placed along the segments that meet there, cutting the corner off.
 * Rings are cut all the way around, while the endpoints of open lines are kept where they are so
 * that adjacent lines stay joined.
 */
public class ChaikinSmoother extends GeometryTransformer {

  private final int iterations;

  private final double factor;

  /**
   * Constructs a {@code ChaikinSmoother} with the specified number of iterations and factor.
   *
   * @param iterations the number of passes; each one roughly doubles the number of vertices
   * @param factor how far along each segment the cut points are placed, in {@code (0, 0.5]}; the
   *        value Chaikin describes is 0.25
   */
  public ChaikinSmoother(int iterations, double factor) {
    if (factor <= 0 || factor > 0.5) {
      throw new IllegalArgumentException("Factor must be in (0, 0.5], got " + factor);
    }
    this.iterations = iterations;
    this.factor = factor;
  }

  /** {@inheritDoc} */
  @Override
  protected CoordinateSequence transformCoordinates(
      CoordinateSequence coordinateSequence,
      Geometry parent) {
    if (coordinateSequence.size() < 2) {
      return coordinateSequence;
    }
    boolean ring = CoordinateSequences.isRing(coordinateSequence);
    Coordinate[] coordinates = coordinateSequence.toCoordinateArray();
    for (int i = 0; i < iterations; i++) {
      coordinates = ring ? cutRing(coordinates) : cutLine(coordinates);
    }
    return new CoordinateArraySequence(coordinates);
  }

  /** Cuts every corner of a ring, whose last coordinate repeats its first. */
  private Coordinate[] cutRing(Coordinate[] ring) {
    int corners = ring.length - 1;
    Coordinate[] cut = new Coordinate[corners * 2 + 1];
    for (int i = 0; i < corners; i++) {
      Coordinate from = ring[i];
      Coordinate to = ring[(i + 1) % corners];
      cut[i * 2] = along(from, to, factor);
      cut[i * 2 + 1] = along(from, to, 1 - factor);
    }
    cut[corners * 2] = cut[0].copy();
    return cut;
  }

  /** Cuts every corner of an open line, keeping its two endpoints. */
  private Coordinate[] cutLine(Coordinate[] line) {
    Coordinate[] cut = new Coordinate[line.length * 2];
    cut[0] = line[0].copy();
    for (int i = 0; i < line.length - 1; i++) {
      cut[i * 2 + 1] = along(line[i], line[i + 1], factor);
      cut[i * 2 + 2] = along(line[i], line[i + 1], 1 - factor);
    }
    cut[cut.length - 1] = line[line.length - 1].copy();
    return cut;
  }

  /** Returns the point at the given fraction of the segment from one coordinate to another. */
  private static Coordinate along(Coordinate from, Coordinate to, double fraction) {
    return new Coordinate(
        from.getX() + fraction * (to.getX() - from.getX()),
        from.getY() + fraction * (to.getY() - from.getY()));
  }
}
