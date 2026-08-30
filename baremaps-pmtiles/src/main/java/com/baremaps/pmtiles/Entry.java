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

/**
 * An entry of a PMTiles directory.
 * <p>
 * A run length greater than zero marks a tile entry that covers the {@code runLength} consecutive
 * tile ids starting at {@code tileId}, and whose blob lies at {@code offset} in the tile data
 * section. A run length of zero marks a pointer to a leaf directory at {@code offset} in the leaf
 * directory section.
 *
 * @param tileId the first tile id covered by this entry
 * @param offset the offset of the blob or leaf directory within its section
 * @param length the length in bytes of the blob or leaf directory
 * @param runLength the number of consecutive tiles sharing this blob, or zero for a leaf pointer
 */
record Entry(long tileId, long offset, long length, long runLength) {

  /** Returns a copy of this entry covering {@code runLength} tiles. */
  Entry withRunLength(long runLength) {
    return new Entry(tileId, offset, length, runLength);
  }
}
