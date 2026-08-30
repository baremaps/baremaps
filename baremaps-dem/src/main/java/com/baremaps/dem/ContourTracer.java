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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.linemerge.LineMerger;

/**
 * Traces the contours of a rectangular grid of elevation values with the marching squares
 * algorithm. Coordinates are expressed in grid space: the sample at index {@code y * width + x}
 * sits at {@code (x, y)}, so the contours of a grid of {@code width} by {@code height} samples span
 * {@code [0, width - 1]} by {@code [0, height - 1]}.
 */
public class ContourTracer {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * How far a crossing is kept away from the corners of a cell. A crossing that lands exactly on a
   * corner belongs to both contours that meet there, which merges them into a single self-touching
   * ring that no longer bounds a valid polygon. The offset is far below the resolution at which
   * contours are ever rendered.
   */
  private static final double EPSILON = 1e-10;

  /**
   * The corners of a cell, as offsets from its lower left sample, in clockwise order. Edge {@code
   * i} of the cell runs from corner {@code i - 1} to corner {@code i}; the walk below relies on
   * that alignment to tell which edge a segment follows from the vertex it ends on.
   */
  private static final int[][] CORNERS = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

  private final double[] grid;

  private final int width;

  private final int height;

  /**
   * Constructs a new {@code ContourTracer} over a copy of the given grid.
   *
   * @param grid the grid of elevation values, in row-major order
   * @param width the width of the grid, in samples
   * @param height the height of the grid, in samples
   */
  public ContourTracer(double[] grid, int width, int height) {
    if (grid == null || grid.length == 0) {
      throw new IllegalArgumentException("Grid array cannot be null or empty");
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive");
    }
    if (grid.length != width * height) {
      throw new IllegalArgumentException("Grid array length does not match width * height");
    }
    this.grid = Arrays.copyOf(grid, grid.length);
    this.width = width;
    this.height = height;
  }

  /**
   * Traces the contour lines at the given level. Contours that reach the edge of the grid are left
   * open there.
   *
   * @param level the elevation level
   * @return the contour lines
   */
  public List<LineString> traceLines(double level) {
    return merge(trace(level, false));
  }

  /**
   * Traces the areas at or above the given level as polygons. Contours that reach the edge of the
   * grid are closed along it, and rings nested inside one another are assembled into polygons with
   * holes.
   *
   * @param level the elevation level
   * @return the contour polygons
   */
  public List<Polygon> tracePolygons(double level) {
    return nest(merge(trace(level, true)));
  }

  /**
   * Collects the contour segments of every cell of the grid.
   *
   * @param level the elevation level
   * @param close whether to close the contours along the edges of the grid
   * @return the contour segments, in no particular order
   */
  private List<LineString> trace(double level, boolean close) {
    var segments = new ArrayList<LineString>();
    for (int y = 0; y < height - 1; y++) {
      for (int x = 0; x < width - 1; x++) {
        traceCell(level, x, y, close, segments);
      }
    }
    return segments;
  }

  /**
   * Walks the boundary of a single cell clockwise and emits the contour segments it carries.
   *
   * <p>
   * The walk visits, for each edge in turn, the point where the contour crosses it (when its two
   * corners straddle the level) and then the corner it ends on (when that corner is at or above the
   * level). Consecutive vertices of the walk are joined: two crossings are joined by a piece of the
   * contour itself, and everything else by a segment running along an edge of the cell. The latter
   * only matter where a cell edge is also an edge of the grid, and only when the contour is being
   * closed into rings; elsewhere they are interior to a contour band and would cancel out against
   * the neighbouring cell.
   *
   * <p>
   * Visiting the corners in a fixed rotation also settles the ambiguity of a saddle cell, where two
   * opposite corners lie above the level: the walk always cuts the two corners below it apart,
   * rather than joining them.
   */
  private void traceCell(double level, int x, int y, boolean close, List<LineString> segments) {
    var inside = new boolean[4];
    for (int i = 0; i < 4; i++) {
      inside[i] = elevation(x + CORNERS[i][0], y + CORNERS[i][1]) >= level;
    }

    var vertices = new ArrayList<Vertex>(6);
    for (int i = 0; i < 4; i++) {
      int previous = (i + 3) % 4;
      if (inside[previous] != inside[i]) {
        vertices.add(new Vertex(crossing(level, x, y, previous, i), i, true));
      }
      if (inside[i]) {
        vertices.add(new Vertex(corner(x, y, i), i, false));
      }
    }

    for (int i = 0; i < vertices.size(); i++) {
      var from = vertices.get(i);
      var to = vertices.get((i + 1) % vertices.size());
      if ((from.crossing() && to.crossing()) || (close && onGridEdge(to.edge(), x, y))) {
        segments.add(GEOMETRY_FACTORY
            .createLineString(new Coordinate[] {from.coordinate(), to.coordinate()}));
      }
    }
  }

  /**
   * A vertex of the walk around a cell: either a corner of the cell that lies at or above the
   * level, or the point where the contour crosses one of its edges. In both cases {@code edge} is
   * the edge of the cell that ends on the vertex, which is the edge a segment arriving there runs
   * along.
   */
  private record Vertex(Coordinate coordinate, int edge, boolean crossing) {
  }

  /** Returns the corner {@code i} of the cell at {@code (x, y)}. */
  private static Coordinate corner(int x, int y, int i) {
    return new Coordinate(x + CORNERS[i][0], y + CORNERS[i][1]);
  }

  /**
   * Returns the point where the level crosses the edge between two corners of the cell at {@code
   * (x, y)}, interpolated linearly between their elevations. Corners of equal elevation are crossed
   * halfway, which both avoids a division by zero and keeps the crossing away from either corner.
   *
   * <p>
   * The corner nearer the origin of the cell is always taken first, whichever way the walk runs
   * over the edge. Two cells sharing an edge have to place their crossing at the very same
   * coordinate, and interpolating from one end or the other gives results that differ in the last
   * bits, which is enough to leave the merged contour open.
   */
  private Coordinate crossing(double level, int x, int y, int from, int to) {
    int near = distance(from) <= distance(to) ? from : to;
    int far = near == from ? to : from;
    int x1 = x + CORNERS[near][0];
    int y1 = y + CORNERS[near][1];
    int x2 = x + CORNERS[far][0];
    int y2 = y + CORNERS[far][1];
    double v1 = elevation(x1, y1);
    double v2 = elevation(x2, y2);
    double t = Math.abs(v2 - v1) < EPSILON ? 0.5 : (level - v1) / (v2 - v1);
    t = Math.clamp(t, EPSILON, 1 - EPSILON);
    return new Coordinate(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
  }

  /** Returns the distance of a corner of a cell from its lower left sample. */
  private static int distance(int corner) {
    return CORNERS[corner][0] + CORNERS[corner][1];
  }

  /** Tells whether the given edge of the cell at {@code (x, y)} is also an edge of the grid. */
  private boolean onGridEdge(int edge, int x, int y) {
    int[] from = CORNERS[(edge + 3) % 4];
    int[] to = CORNERS[edge];
    if (from[0] == to[0]) {
      int column = x + from[0];
      return column == 0 || column == width - 1;
    }
    int row = y + from[1];
    return row == 0 || row == height - 1;
  }

  private double elevation(int x, int y) {
    return grid[y * width + x];
  }

  /**
   * Joins the segments into the longest possible lines. Merging leaves a vertex wherever two
   * segments met, including where they were collinear, so the result is passed through the fixer to
   * drop the repeated and degenerate ones.
   */
  private static List<LineString> merge(List<LineString> segments) {
    var merger = new LineMerger();
    merger.add(segments);
    var lines = new ArrayList<LineString>();
    for (Object merged : merger.getMergedLineStrings()) {
      if (new GeometryFixer((Geometry) merged).getResult() instanceof LineString line
          && !line.isEmpty()) {
        lines.add(line);
      }
    }
    return lines;
  }

  /**
   * Assembles closed contour rings into polygons. A ring can only ever be a hole of a larger ring,
   * so the rings are taken from the largest to the smallest: each one becomes a shell and claims
   * the rings it directly contains as its holes. A ring nested inside one of those holes is left
   * behind and becomes a shell of its own on a later pass, which is what makes an island in a lake
   * come out as a separate polygon.
   */
  private static List<Polygon> nest(List<LineString> rings) {
    var candidates = rings.stream()
        .map(ring -> GEOMETRY_FACTORY.createPolygon(ring.getCoordinates()))
        .map(polygon -> new GeometryFixer(polygon).getResult())
        .flatMap(ContourTracer::polygons)
        .sorted(Comparator.comparingDouble(Polygon::getArea).reversed())
        .collect(ArrayList<Polygon>::new, ArrayList::add, ArrayList::addAll);

    var polygons = new ArrayList<Polygon>();
    while (!candidates.isEmpty()) {
      var shell = candidates.remove(0);
      var holes = new ArrayList<Polygon>();
      candidates.removeIf(candidate -> {
        if (!shell.contains(candidate) || holes.stream().anyMatch(h -> h.contains(candidate))) {
          return false;
        }
        holes.add(candidate);
        return true;
      });
      polygons.add(GEOMETRY_FACTORY.createPolygon(
          shell.getExteriorRing(),
          holes.stream().map(Polygon::getExteriorRing).toArray(LinearRing[]::new)));
    }
    return polygons;
  }

  /**
   * Returns the polygons of a geometry. Fixing a ring that crosses itself splits it into several
   * polygons, all of which are part of the contour.
   */
  private static Stream<Polygon> polygons(Geometry geometry) {
    return IntStream.range(0, geometry.getNumGeometries())
        .mapToObj(geometry::getGeometryN)
        .filter(Polygon.class::isInstance)
        .map(Polygon.class::cast)
        .filter(polygon -> !polygon.isEmpty());
  }
}
