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

package com.baremaps.data.algorithm;

import com.baremaps.data.collection.DataList;
import java.util.Comparator;
import java.util.function.Function;

/** Binary search over sorted {@link DataList}s. */
public class BinarySearch {

  private BinarySearch() {
    // Prevent instantiation
  }

  /** Returns the index of the value in the sorted list, or -1 if it is absent. */
  public static <E> long binarySearch(DataList<E> list, E value, Comparator<E> comparator) {
    return binarySearch(list, Function.identity(), value, comparator);
  }

  /**
   * Returns the index of the element whose extracted attribute equals the value, in a list sorted
   * by that attribute, or -1 if it is absent.
   */
  public static <E, A> long binarySearch(
      DataList<E> list, Function<E, A> extractor, A value, Comparator<A> comparator) {
    long lo = 0;
    long hi = list.size() - 1;
    while (lo <= hi) {
      long mid = (lo + hi) >>> 1;
      int cmp = comparator.compare(extractor.apply(list.get(mid)), value);
      if (cmp < 0) {
        lo = mid + 1;
      } else if (cmp > 0) {
        hi = mid - 1;
      } else {
        return mid;
      }
    }
    return -1;
  }
}
