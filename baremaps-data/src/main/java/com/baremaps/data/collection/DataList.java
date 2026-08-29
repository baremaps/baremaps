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

package com.baremaps.data.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** A collection whose elements are addressed by a {@code long} index. */
public interface DataList<E> extends DataCollection<E> {

  /** Appends a value and returns its index. */
  long addIndexed(E value);

  @Override
  default boolean add(E value) {
    addIndexed(value);
    return true;
  }

  /**
   * Replaces the value at the given index.
   *
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  void set(long index, E value);

  /**
   * Returns the value at the given index.
   *
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  E get(long index);

  @Override
  default Iterator<E> iterator() {
    return new Iterator<>() {
      private final long size = size();
      private long index = 0;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public E next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return get(index++);
      }
    };
  }
}
