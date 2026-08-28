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

package com.baremaps.tilestore;



import java.util.function.Predicate;

/** A predicate that filters tiles according to the index of a batch. */
public class TileBatchPredicate implements Predicate<TileCoord> {

  private final int batchArraySize;

  private final int batchArrayIndex;

  /**
   * Constructs a {@code TileBatchPredicate}.
   *
   * @param batchArraySize the size of the batch array
   * @param batchArrayIndex the index of the batch in the array
   */
  public TileBatchPredicate(int batchArraySize, int batchArrayIndex) {
    this.batchArraySize = batchArraySize;
    this.batchArrayIndex = batchArrayIndex;
  }

  /**
   * Returns true if the tile belongs to the current batch.
   *
   * @param tileCoord the tile coordinate
   * @return the result
   */
  @Override
  public boolean test(TileCoord tileCoord) {
    return batchArraySize <= 1 || tileCoord.index() % batchArraySize == batchArrayIndex;
  }
}
