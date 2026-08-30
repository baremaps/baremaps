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

package com.baremaps.pmtiles;

import java.io.IOException;

/**
 * The formats a PMTiles archive can store its tiles in.
 * <p>
 * <strong>The ordinal of each constant is the value stored in the header.</strong> Reordering,
 * inserting or removing a constant silently changes how every existing archive is interpreted, so
 * new formats may only be appended.
 */
public enum TileType {

  UNKNOWN,
  MVT,
  PNG,
  JPEG,
  WEBP,
  AVIF;

  /**
   * Returns the tile type stored under the given header value.
   *
   * @param value the value read from the header
   * @return the corresponding tile type
   * @throws IOException if the value is not a known tile type
   */
  static TileType forHeaderValue(int value) throws IOException {
    var values = values();
    if (value < 0 || value >= values.length) {
      throw new IOException("Unknown tile type value: " + value);
    }
    return values[value];
  }
}
