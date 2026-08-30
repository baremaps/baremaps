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

import java.awt.image.BufferedImage;

/**
 * The {@code Martini} class implements the MARTINI algorithm for generating 3D terrain meshes from
 * height data. A grid is meshed by a hierarchy of right triangles, each of which may be split in
 * two at the midpoint of its hypotenuse; the hierarchy only depends on the size of the grid, so it
 * is built once here and shared by every tile of that size.
 *
 * @see <a href="https://github.com/mapbox/martini">Martini GitHub</a>
 */
public class Martini {

  private final int gridSize;
  private final int numTriangles;
  private final int numParentTriangles;
  private final int[] baseCoords;

  /**
   * Constructs a new {@code Martini} instance with the specified grid size.
   *
   * @param gridSize the grid size (must be 2^n+1)
   * @throws IllegalArgumentException if the grid size is invalid
   */
  public Martini(int gridSize) {
    this.gridSize = gridSize;

    int tileSize = gridSize - 1;
    if ((tileSize & (tileSize - 1)) != 0) {
      throw new IllegalArgumentException("Expected grid size to be 2^n+1, got " + gridSize + ".");
    }

    this.numTriangles = tileSize * tileSize * 2 - 2;
    this.numParentTriangles = this.numTriangles - tileSize * tileSize;

    // Triangles are numbered breadth first from the two halves of the grid, so that the two
    // children of triangle id are 2 * id and 2 * id + 1. Reading the bits of an id from the most
    // significant one down therefore replays the sequence of splits that leads to it, which is how
    // the two ends of its hypotenuse are recovered here.
    this.baseCoords = new int[this.numTriangles * 4];
    for (int i = 0; i < this.numTriangles; i++) {
      int id = i + 2;
      int ax = 0;
      int ay = 0;
      int bx = 0;
      int by = 0;
      int cx = 0;
      int cy = 0;
      if ((id & 1) != 0) {
        bx = by = cx = tileSize;
      } else {
        ax = ay = cy = tileSize;
      }
      while ((id >>= 1) > 1) {
        int mx = (ax + bx) >> 1;
        int my = (ay + by) >> 1;

        if ((id & 1) != 0) {
          bx = ax;
          by = ay;
          ax = cx;
          ay = cy;
        } else {
          ax = bx;
          ay = by;
          bx = cx;
          by = cy;
        }
        cx = mx;
        cy = my;
      }
      int k = i * 4;
      this.baseCoords[k] = ax;
      this.baseCoords[k + 1] = ay;
      this.baseCoords[k + 2] = bx;
      this.baseCoords[k + 3] = by;
    }
  }

  /**
   * Decodes a Terrain-RGB image into a terrain grid one sample wider and taller than the image, as
   * the mesh is built over the corners of the pixels rather than their centers.
   *
   * @param image the input image
   * @return a double array representing the terrain grid
   */
  public static double[] createGrid(BufferedImage image) {
    int tileSize = image.getWidth();
    int gridSize = tileSize + 1;
    double[] terrain = new double[gridSize * gridSize];

    for (int y = 0; y < tileSize; y++) {
      for (int x = 0; x < tileSize; x++) {
        terrain[y * gridSize + x] = ElevationUtils.rgbToElevation(image.getRGB(x, y));
      }
    }

    // Repeat the last row and column into the extra samples, so that the edge of the mesh follows
    // the edge of the image instead of dropping to zero.
    for (int x = 0; x < gridSize - 1; x++) {
      terrain[gridSize * (gridSize - 1) + x] = terrain[gridSize * (gridSize - 2) + x];
    }
    for (int y = 0; y < gridSize; y++) {
      terrain[gridSize * y + gridSize - 1] = terrain[gridSize * y + gridSize - 2];
    }

    return terrain;
  }

  /**
   * Creates a new {@code Tile} instance with the specified terrain data.
   *
   * @param terrain the terrain data
   * @return the tile
   * @throws IllegalArgumentException if the terrain data is invalid
   */
  public Tile createTile(double[] terrain) {
    return new Tile(terrain);
  }

  /**
   * A tile of terrain data, holding for every possible vertex of the mesh the error that leaving it
   * out would introduce.
   */
  public class Tile {

    private final double[] errors;

    private Tile(double[] terrain) {
      if (terrain.length != gridSize * gridSize) {
        throw new IllegalArgumentException(
            "Expected terrain data of length " + (gridSize * gridSize) + " (" + gridSize + " x "
                + gridSize + "), got " + terrain.length + ".");
      }

      // Walking the triangles from the smallest to the largest lets the error of a triangle absorb
      // the errors of its two children, which are already known by the time it is reached. A
      // triangle may then be left unsplit as soon as its own error is acceptable, without having to
      // look any deeper.
      this.errors = new double[terrain.length];
      for (int i = numTriangles - 1; i >= 0; i--) {
        int k = i * 4;
        int ax = baseCoords[k];
        int ay = baseCoords[k + 1];
        int bx = baseCoords[k + 2];
        int by = baseCoords[k + 3];
        int mx = (ax + bx) >> 1;
        int my = (ay + by) >> 1;
        int cx = mx + my - ay;
        int cy = my + ax - mx;

        double interpolatedHeight = (terrain[ay * gridSize + ax] + terrain[by * gridSize + bx]) / 2;
        int middleIndex = my * gridSize + mx;
        double middleError = Math.abs(interpolatedHeight - terrain[middleIndex]);

        errors[middleIndex] = Math.max(errors[middleIndex], middleError);

        if (i < numParentTriangles) {
          int leftChildIndex = ((ay + cy) >> 1) * gridSize + ((ax + cx) >> 1);
          int rightChildIndex = ((by + cy) >> 1) * gridSize + ((bx + cx) >> 1);
          errors[middleIndex] = Math.max(errors[middleIndex],
              Math.max(errors[leftChildIndex], errors[rightChildIndex]));
        }
      }
    }

    /**
     * Returns the coarsest mesh of the tile that stays within the specified error. The tile may be
     * meshed again at a different error, each call starting from a clean slate.
     *
     * @param maxError the maximum error
     * @return the mesh
     */
    public Mesh getMesh(double maxError) {
      return new MeshBuilder(errors, gridSize, maxError).build();
    }
  }

  /**
   * Builds one mesh out of the errors of a tile. The two halves of the grid are walked twice: once
   * to count the vertices and triangles the walk yields, and once to fill the arrays sized from
   * that count.
   */
  private static class MeshBuilder {

    private final double[] errors;
    private final int gridSize;
    private final double maxError;

    /** The one-based position of each vertex in the mesh, or zero for the vertices left out. */
    private final int[] indices;

    private int numVertices;
    private int numTriangles;
    private int[] vertices;
    private int[] triangles;
    private int triangleIndex;

    private MeshBuilder(double[] errors, int gridSize, double maxError) {
      this.errors = errors;
      this.gridSize = gridSize;
      this.maxError = maxError;
      this.indices = new int[gridSize * gridSize];
    }

    private Mesh build() {
      int max = gridSize - 1;

      count(0, 0, max, max, max, 0);
      count(max, max, 0, 0, 0, max);

      vertices = new int[numVertices * 2];
      triangles = new int[numTriangles * 3];
      emit(0, 0, max, max, max, 0);
      emit(max, max, 0, 0, 0, max);

      return new Mesh(vertices, triangles);
    }

    /**
     * Tells whether the triangle {@code (a, b, c)} has to be split at the midpoint of its
     * hypotenuse {@code (a, b)}, which is the case as long as it can still be split and the error
     * at that midpoint is too large.
     */
    private boolean split(int ax, int ay, int bx, int by, int cx, int cy) {
      return Math.abs(ax - cx) + Math.abs(ay - cy) > 1
          && errors[((ay + by) >> 1) * gridSize + ((ax + bx) >> 1)] > maxError;
    }

    private void count(int ax, int ay, int bx, int by, int cx, int cy) {
      if (split(ax, ay, bx, by, cx, cy)) {
        int mx = (ax + bx) >> 1;
        int my = (ay + by) >> 1;
        count(cx, cy, ax, ay, mx, my);
        count(bx, by, cx, cy, mx, my);
      } else {
        index(ax, ay);
        index(bx, by);
        index(cx, cy);
        numTriangles++;
      }
    }

    /** Assigns a position in the mesh to a vertex, unless it already has one. */
    private void index(int x, int y) {
      if (indices[y * gridSize + x] == 0) {
        indices[y * gridSize + x] = ++numVertices;
      }
    }

    private void emit(int ax, int ay, int bx, int by, int cx, int cy) {
      if (split(ax, ay, bx, by, cx, cy)) {
        int mx = (ax + bx) >> 1;
        int my = (ay + by) >> 1;
        emit(cx, cy, ax, ay, mx, my);
        emit(bx, by, cx, cy, mx, my);
      } else {
        int a = vertex(ax, ay);
        int b = vertex(bx, by);
        int c = vertex(cx, cy);
        triangles[triangleIndex++] = a;
        triangles[triangleIndex++] = b;
        triangles[triangleIndex++] = c;
      }
    }

    /** Writes a vertex at the position assigned to it by {@link #count} and returns it. */
    private int vertex(int x, int y) {
      int index = indices[y * gridSize + x] - 1;
      vertices[2 * index] = x;
      vertices[2 * index + 1] = y;
      return index;
    }
  }

  /**
   * A mesh of vertices and triangles. The vertices are the grid coordinates of the mesh points, two
   * values per point; the triangles are indices into them, three per triangle.
   */
  public record Mesh(int[] vertices, int[] triangles) {
  }
}
