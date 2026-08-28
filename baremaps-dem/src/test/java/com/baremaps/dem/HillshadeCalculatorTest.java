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

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HillshadeCalculatorTest {

  @Test
  @DisplayName("Test hillShade with valid input")
  void testHillShadeValidInput() {
    double[] dem = {
        1, 2, 3,
        4, 5, 6,
        7, 8, 9
    };
    int width = 3;
    int height = 3;
    double sunAltitude = 45;
    double sunAzimuth = 315;

    double[] result =
        new HillshadeCalculator(dem, width, height, 1).calculate(sunAltitude, sunAzimuth);

    assertNotNull(result);
    assertEquals(dem.length, result.length);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidInput")
  @DisplayName("Test hillShade with invalid input")
  void testHillShadeInvalidInput(double[] dem, int width, int height, double sunAltitude,
      double sunAzimuth, Class<? extends Exception> expectedException) {
    assertThrows(expectedException,
        () -> new HillshadeCalculator(dem, width, height, 1).calculate(sunAltitude,
            sunAzimuth));
  }

  @ParameterizedTest
  @MethodSource("provideInvalidInput")
  @DisplayName("Test hillShadeEnhanced with invalid input")
  void testHillShadeEnhancedInvalidInput(double[] dem, int width, int height, double sunAltitude,
      double sunAzimuth, Class<? extends Exception> expectedException) {
    assertThrows(expectedException,
        () -> new HillshadeCalculator(dem, width, height, 1).calculate(sunAltitude,
            sunAzimuth));
  }

  private static Stream<Arguments> provideInvalidInput() {
    return Stream.of(
        Arguments.of(null, 3, 3, 45, 315, IllegalArgumentException.class),
        Arguments.of(new double[0], 3, 3, 45, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 0, 3, 45, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 3, 0, 45, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 2, 2, 45, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 3, 3, -1, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 3, 3, 91, 315, IllegalArgumentException.class),
        Arguments.of(new double[9], 3, 3, 45, -1, IllegalArgumentException.class),
        Arguments.of(new double[9], 3, 3, 45, 361, IllegalArgumentException.class));
  }

  @Test
  @DisplayName("Test hillShade output range")
  void testHillShadeOutputRange() {
    double[] dem = new double[100];
    for (int i = 0; i < dem.length; i++) {
      dem[i] = Math.random() * 1000;
    }
    int width = 10;
    int height = 10;
    double sunAltitude = 45;
    double sunAzimuth = 315;

    double[] result =
        new HillshadeCalculator(dem, width, height, 1).calculate(sunAltitude, sunAzimuth);

    for (double value : result) {
      assertTrue(value >= 0 && value <= 255, "Hillshade value should be between 0 and 255");
    }
  }
}
