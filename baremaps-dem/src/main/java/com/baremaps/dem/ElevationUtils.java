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
import java.util.function.IntToDoubleFunction;

/**
 * Converts elevations between the grids this module operates on and the pixel encodings terrain
 * tiles are published in.
 */
public class ElevationUtils {

  /** Terrain-RGB stores decimeters above a datum 10000 meters below sea level. */
  private static final double RGB_UNITS_PER_METER = 10.0;
  private static final double RGB_OFFSET = 10000.0;

  /**
   * Terrarium stores meters above a datum 32768 meters below sea level, with the whole meters
   * spread over the red and green bands and the fraction of a meter in the blue one.
   */
  private static final double TERRARIUM_OFFSET = 32768.0;

  private ElevationUtils() {
    // Prevent instantiation
  }

  /**
   * Decodes an image into a grid of elevation values, in row-major order.
   *
   * @param image the image
   * @param pixelToElevation the encoding of the image, such as {@link #rgbToElevation}
   * @return the elevation grid
   */
  public static double[] imageToGrid(BufferedImage image, IntToDoubleFunction pixelToElevation) {
    int width = image.getWidth();
    int height = image.getHeight();
    double[] grid = new double[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        grid[y * width + x] = pixelToElevation.applyAsDouble(image.getRGB(x, y));
      }
    }
    return grid;
  }

  /**
   * Clamps the values of a grid to the specified range. Elevation models carry no-data markers and
   * artefacts far outside the range of real elevations, which would otherwise dominate whatever is
   * derived from the grid.
   *
   * @param grid the grid
   * @param min the minimum value
   * @param max the maximum value
   * @return the clamped grid
   */
  public static double[] clampGrid(double[] grid, double min, double max) {
    double[] clamped = new double[grid.length];
    for (int i = 0; i < grid.length; i++) {
      clamped[i] = Math.clamp(grid[i], min, max);
    }
    return clamped;
  }

  /**
   * Decodes a pixel of a Terrain-RGB tile into an elevation.
   *
   * @param rgb the pixel value
   * @return the elevation, in meters
   */
  public static double rgbToElevation(int rgb) {
    return (rgb & 0xFFFFFF) / RGB_UNITS_PER_METER - RGB_OFFSET;
  }

  /**
   * Encodes an elevation into a pixel of a Terrain-RGB tile.
   *
   * @param elevation the elevation, in meters
   * @return the pixel value
   */
  public static int elevationToRgb(double elevation) {
    return (int) ((elevation + RGB_OFFSET) * RGB_UNITS_PER_METER) & 0xFFFFFF;
  }

  /**
   * Decodes a pixel of a Terrarium tile into an elevation.
   *
   * @param rgb the pixel value
   * @return the elevation, in meters
   */
  public static double terrariumToElevation(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return (r * 256.0 + g + b / 256.0) - TERRARIUM_OFFSET;
  }

  /**
   * Encodes an elevation into a pixel of a Terrarium tile.
   *
   * @param elevation the elevation, in meters
   * @return the pixel value
   */
  public static int elevationToTerrarium(double elevation) {
    double value = elevation + TERRARIUM_OFFSET;
    int meters = (int) value;
    int fraction = (int) ((value - meters) * 256.0);
    return ((meters >> 8) & 0xFF) << 16 | (meters & 0xFF) << 8 | (fraction & 0xFF);
  }
}
