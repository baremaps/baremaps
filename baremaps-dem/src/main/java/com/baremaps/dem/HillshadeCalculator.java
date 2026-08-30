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

/**
 * Shades a digital elevation model as if it were lit by a distant light source, the effect commonly
 * known as hillshading.
 */
public class HillshadeCalculator {

  private static final double EARTH_RADIUS = 6378137; // in meters
  private static final int TILE_SIZE = 256; // in pixels

  private final double[] dem;
  private final int width;
  private final int height;
  private final double cellSize;

  /**
   * Constructs a {@code HillshadeCalculator} with the specified DEM, width, height, and cell size.
   *
   * @param dem the digital elevation model, in row-major order
   * @param width the width of the DEM, in samples
   * @param height the height of the DEM, in samples
   * @param cellSize the ground distance between two samples, in the unit of the elevations
   */
  public HillshadeCalculator(double[] dem, int width, int height, double cellSize) {
    if (dem == null || dem.length == 0) {
      throw new IllegalArgumentException("Grid array cannot be null or empty");
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive");
    }
    if (dem.length != width * height) {
      throw new IllegalArgumentException("Grid array length does not match width * height");
    }
    this.dem = dem;
    this.width = width;
    this.height = height;
    this.cellSize = cellSize;
  }

  /**
   * Shades every cell of the model for a light source at the specified altitude and azimuth.
   *
   * @param altitude the altitude of the light source above the horizon, in degrees
   * @param azimuth the compass bearing the light source shines from, in degrees
   * @return the shade of every cell, from 0 (in shadow) to 255 (fully lit)
   */
  public double[] calculate(double altitude, double azimuth) {
    if (altitude < 0 || altitude > 90) {
      throw new IllegalArgumentException("Altitude must be between 0 and 90 degrees");
    }
    if (azimuth < 0 || azimuth > 360) {
      throw new IllegalArgumentException("Azimuth must be between 0 and 360 degrees");
    }

    // Compass bearings run clockwise from north, whereas the aspect computed below runs
    // counterclockwise from east; the light source is turned into the latter convention once,
    // rather than the aspect of every cell into the former.
    double azimuthRad = Math.toRadians(360.0 - azimuth + 90.0);
    double zenithRad = Math.toRadians(90.0 - altitude);

    double[] hillshade = new double[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        hillshade[y * width + x] = shade(x, y, zenithRad, azimuthRad);
      }
    }
    return hillshade;
  }

  /**
   * Shades a single cell. The slope and the aspect of the surface are estimated from the eight
   * neighbours of the cell, weighting the four that share an edge with it twice as much as the four
   * that only share a corner, and the cell is then lit by the cosine of the angle between its
   * normal and the light source.
   */
  private double shade(int x, int y, double zenithRad, double azimuthRad) {
    double topLeft = elevation(x - 1, y - 1);
    double top = elevation(x, y - 1);
    double topRight = elevation(x + 1, y - 1);
    double left = elevation(x - 1, y);
    double right = elevation(x + 1, y);
    double bottomLeft = elevation(x - 1, y + 1);
    double bottom = elevation(x, y + 1);
    double bottomRight = elevation(x + 1, y + 1);

    double dzdx = ((topRight + 2 * right + bottomRight)
        - (topLeft + 2 * left + bottomLeft)) / (8 * cellSize);
    double dzdy = ((bottomLeft + 2 * bottom + bottomRight)
        - (topLeft + 2 * top + topRight)) / (8 * cellSize);

    double slopeRad = Math.atan(Math.sqrt(dzdx * dzdx + dzdy * dzdy));
    double aspectRad = Math.atan2(dzdy, -dzdx);

    double shade = 255.0 * (Math.cos(zenithRad) * Math.cos(slopeRad)
        + Math.sin(zenithRad) * Math.sin(slopeRad) * Math.cos(azimuthRad - aspectRad));
    return Math.clamp(shade, 0.0, 255.0);
  }

  /**
   * Returns the elevation at the specified coordinates, repeating the border of the model for
   * coordinates that fall outside of it. The cells along the border would otherwise have no slope
   * to compute from, and repeating the border makes them shade like their neighbours instead of
   * like a cliff.
   */
  private double elevation(int x, int y) {
    return dem[Math.clamp(y, 0, height - 1) * width + Math.clamp(x, 0, width - 1)];
  }

  /**
   * Inverts a grid of shades, so that the contours of the shadows can be traced the same way as the
   * contours of the lit slopes.
   *
   * @param hillshade the shades to invert
   * @return the inverted shades
   */
  public static double[] invert(double[] hillshade) {
    double[] inverted = new double[hillshade.length];
    for (int i = 0; i < hillshade.length; i++) {
      inverted[i] = 255.0 - hillshade[i];
    }
    return inverted;
  }

  /**
   * Returns the ground distance covered by one pixel of a web mercator tile at the specified zoom
   * level, which is the cell size to shade such a tile with.
   *
   * @param zoomLevel the zoom level
   * @return the resolution, in meters per pixel
   */
  public static double getResolution(int zoomLevel) {
    return (2 * Math.PI * EARTH_RADIUS) / (TILE_SIZE * Math.pow(2, zoomLevel));
  }
}
