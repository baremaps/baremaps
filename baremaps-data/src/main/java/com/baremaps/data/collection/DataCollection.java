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

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection of elements that can be iterated over. Similar to {@link java.util.Collection} but
 * supports up to {@link Long#MAX_VALUE} elements.
 *
 * @param <E> The type of elements in the collection
 */
public interface DataCollection<E> extends Iterable<E>, AutoCloseable {

  /**
   * Returns the number of elements in this collection.
   *
   * @return the number of elements
   */
  long size();

  /**
   * Returns true if this collection contains no elements.
   *
   * @return true if this collection is empty
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns an iterator over the elements in this collection.
   *
   * @return an iterator
   */
  @Override
  Iterator<E> iterator();

  /**
   * Returns a spliterator over the elements in this collection.
   *
   * @return a spliterator
   */
  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
  }

  /**
   * Returns a sequential stream over the elements in this collection.
   *
   * @return a sequential stream
   */
  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Returns a parallel stream over the elements in this collection.
   *
   * @return a parallel stream
   */
  default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }

  /**
   * Adds an element to this collection.
   *
   * @param e the element to add
   * @return true if the collection changed as a result of the call
   */
  default boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Adds all elements from the specified collection to this collection.
   *
   * @param c the collection of elements to add
   * @return true if this collection changed as a result of the call
   */
  default boolean addAll(Iterable<? extends E> c) {
    boolean modified = false;
    for (E e : c) {
      if (add(e)) {
        modified = true;
      }
    }
    return modified;
  }

  /**
   * Returns true if this collection contains the specified element.
   *
   * @param o the element to check for
   * @return true if this collection contains the element
   */
  default boolean contains(Object o) {
    for (E e : this) {
      if (e.equals(o)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if this collection contains all elements in the specified collection.
   *
   * @param c the collection to check against
   * @return true if this collection contains all elements in the specified collection
   */
  default boolean containsAll(Iterable<?> c) {
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Removes all elements from this collection.
   */
  void clear();

}
