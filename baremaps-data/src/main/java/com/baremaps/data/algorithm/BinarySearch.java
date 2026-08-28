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

/**
 * A binary search algorithm.
 */
public class BinarySearch {

  private BinarySearch() {
    // Prevent instantiation
  }

  /**
   * Returns the index of the search key, if it is contained in the list; null otherwise.
   *
   * @param list the list to search
   * @param value the value to search for
   * @param comparator the comparator
   * @return the index of the search key
   * @param <E> the type of the elements in the list
   */
  public static <E> Long binarySearch(DataList<E> list, E value, Comparator<E> comparator) {
    return binarySearch(list, value, comparator, 0, list.size() - 1l);
  }

  /**
   * Returns the index of the search key, if it is contained in the list; null otherwise.
   *
   * @param list the list to search
   * @param value the value to search for
   * @param comparator the comparator
   * @param fromIndex the low index
   * @param toIndex the high index
   * @return the index of the search key
   * @param <E> the type of the elements in the list
   */
  public static <E> Long binarySearch(DataList<E> list, E value, Comparator<E> comparator,
      long fromIndex, long toIndex) {
    long lo = fromIndex;
    long hi = toIndex;
    while (lo <= hi) {
      long mi = (lo + hi) >>> 1;
      E e = list.get(mi);
      int cmp = comparator.compare(e, value);
      if (cmp < 0) {
        lo = mi + 1;
      } else if (cmp > 0) {
        hi = mi - 1;
      } else {
        return mi; // key found
      }
    }
    return null; // key not found.
  }

  /**
   * Returns the value corresponding the search key, if it is contained in the list; null otherwise.
   *
   * @param list the list to search
   * @param extractor the attribute extractor
   * @param value the value to search for
   * @param comparator the comparator
   * @return the index of the search key
   * @param <E> the type of the elements in the list
   */
  public static <E, A> E binarySearchAttribute(
      DataList<E> list,
      Function<E, A> extractor,
      A value,
      Comparator<A> comparator) {
    long lo = 0;
    long hi = list.size() - 1l;
    while (lo <= hi) {
      long mi = (lo + hi) >>> 1;
      E e = list.get(mi);
      A a = extractor.apply(e);
      int cmp = comparator.compare(a, value);
      if (cmp < 0) {
        lo = mi + 1;
      } else if (cmp > 0) {
        hi = mi - 1;
      } else {
        return e; // key found
      }
    }
    return null; // key not found.
  }

  /**
   * Returns the value corresponding the search key, if it is contained in the list; null otherwise.
   *
   * @param list the list to search
   * @param extractor the attribute extractor
   * @param value the value to search for
   * @param comparator the comparator
   * @param fromIndex the low index
   * @param toIndex the high index
   * @return the index of the search key
   * @param <E> the type of the elements in the list
   */
  public static <E, A> E binarySearchAttribute(
      DataList<E> list,
      Function<E, A> extractor,
      A value,
      Comparator<A> comparator,
      long fromIndex,
      long toIndex) {
    long lo = fromIndex;
    long hi = toIndex;
    while (lo <= hi) {
      long mi = (lo + hi) >>> 1;
      E e = list.get(mi);
      A a = extractor.apply(e);
      int cmp = comparator.compare(a, value);
      if (cmp < 0) {
        lo = mi + 1;
      } else if (cmp > 0) {
        hi = mi - 1;
      } else {
        return e; // key found
      }
    }
    return null; // key not found.
  }

}
