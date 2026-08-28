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

package com.baremaps.tilestore.raster;

import java.awt.*;
import java.nio.DoubleBuffer;
import org.apache.sis.image.Interpolation;

/**
 * An {@code Interpolation} that performs bicubic interpolation.
 */
class BicubicInterpolation extends Interpolation {

  /** {@inheritDoc} */
  @Override
  public Dimension getSupportSize() {
    return new Dimension(4, 4);
  }

  /** {@inheritDoc} */
  @Override
  public void interpolate(DoubleBuffer source, int numBands, double xfrac, double yfrac,
      double[] writeTo, int writeToOffset) {
    // Initialize arrays for the intermediate computations
    double[] colValues = new double[4];
    double[] rowValues = new double[4];

    // Iterate over each band
    for (int band = 0; band < numBands; band++) {
      // Interpolate each row
      for (int j = 0; j < 4; j++) {
        for (int i = 0; i < 4; i++) {
          rowValues[i] = source.get(band + i * numBands + j * numBands * 4);
        }
        colValues[j] = cubicInterpolate(rowValues, xfrac);
      }

      // Interpolate the final value for this band
      writeTo[writeToOffset + band] = cubicInterpolate(colValues, yfrac);
    }
  }

  private double cubicInterpolate(double[] p, double x) {
    // Using the cubic interpolation formula
    return p[1] + 0.5 * x * (p[2] - p[0] + x
        * (2.0 * p[0] - 5.0 * p[1] + 4.0 * p[2] - p[3] + x * (3.0 * (p[1] - p[2]) + p[3] - p[0])));
  }

}
