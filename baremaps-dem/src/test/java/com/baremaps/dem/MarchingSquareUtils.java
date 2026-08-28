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

import java.util.List;

public class MarchingSquareUtils {

  public static final double[] CASE_00 = {
      0, 0,
      0, 0,
  };

  public static final double[] CASE_01 = {
      1, 0,
      0, 0,
  };

  public static final double[] CASE_02 = {
      0, 1,
      0, 0,
  };

  public static final double[] CASE_03 = {
      1, 1,
      0, 0,
  };

  public static final double[] CASE_04 = {
      0, 0,
      0, 1,
  };

  public static final double[] CASE_05 = {
      1, 0,
      0, 1,
  };

  public static final double[] CASE_06 = {
      0, 1,
      0, 1,
  };

  public static final double[] CASE_07 = {
      1, 1,
      0, 1,
  };

  public static final double[] CASE_08 = {
      0, 0,
      1, 0,
  };

  public static final double[] CASE_09 = {
      1, 0,
      1, 0,
  };

  public static final double[] CASE_10 = {
      0, 1,
      1, 0,
  };

  public static final double[] CASE_11 = {
      1, 1,
      1, 0,
  };

  public static final double[] CASE_12 = {
      0, 0,
      1, 1,
  };

  public static final double[] CASE_13 = {
      1, 0,
      1, 1,
  };

  public static final double[] CASE_14 = {
      0, 1,
      1, 1,
  };

  public static final double[] CASE_15 = {
      1, 1,
      1, 1,
  };

  public static final List<double[]> CASES = List.of(
      CASE_00, CASE_01, CASE_02, CASE_03,
      CASE_04, CASE_05, CASE_06, CASE_07,
      CASE_08, CASE_09, CASE_10, CASE_11,
      CASE_12, CASE_13, CASE_14, CASE_15);

  public static final double[] buffer(double[] grid) {
    return new double[] {
        0, 0, 0, 0,
        0, grid[0], grid[1], 0,
        0, grid[2], grid[3], 0,
        0, 0, 0, 0,
    };
  }

  public static final List<double[]> BUFFERED_CASES = List.of(
      buffer(CASE_00), buffer(CASE_01), buffer(CASE_02), buffer(CASE_03),
      buffer(CASE_04), buffer(CASE_05), buffer(CASE_06), buffer(CASE_07),
      buffer(CASE_08), buffer(CASE_09), buffer(CASE_10), buffer(CASE_11),
      buffer(CASE_12), buffer(CASE_13), buffer(CASE_14), buffer(CASE_15));



}
