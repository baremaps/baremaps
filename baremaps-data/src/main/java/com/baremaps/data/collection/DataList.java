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

/**
 * A sequence of elements that can be accessed by index. Similar to {@link java.util.List} but
 * supports up to {@link Long#MAX_VALUE} elements.
 *
 * @param <E> The type of elements in the list
 */
public interface DataList<E> extends DataCollection<E> {

  /**
   * Adds an element to this list and returns its index.
   *
   * @param value the element to add
   * @return the index of the added element
   */
  long addIndexed(E value);

  /** {@inheritDoc} */
  @Override
  default boolean add(E value) {
    addIndexed(value);
    return true;
  }

  /**
   * Sets the element at the specified index.
   *
   * @param index the index
   * @param value the element to set
   */
  void set(long index, E value);

  /**
   * Returns the element at the specified index.
   *
   * @param index the index
   * @return the element at the specified index
   */
  E get(long index);

  /** {@inheritDoc} */
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
