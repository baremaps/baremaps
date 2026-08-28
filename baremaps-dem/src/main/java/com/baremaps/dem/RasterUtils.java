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

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Provides utility methods for processing raster images.
 */
public class RasterUtils {

  private RasterUtils() {
    // Prevent instantiation
  }

  /**
   * Resizes a BufferedImage to the specified dimensions.
   *
   * @param originalImage The original BufferedImage
   * @param targetWidth The target width
   * @param targetHeight The target height
   * @return The resized BufferedImage
   */
  public static BufferedImage resizeImage(
      BufferedImage originalImage, int targetWidth, int targetHeight) {
    Image resultingImage =
        originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
    BufferedImage outputImage =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
    outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
    return outputImage;
  }
}
