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
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection that can hold more than {@link Integer#MAX_VALUE} elements, typically outside the
 * heap. Elements are never null.
 */
public interface DataCollection<E> extends Iterable<E>, AutoCloseable {

  long size();

  default boolean isEmpty() {
    return size() == 0;
  }

  @Override
  Iterator<E> iterator();

  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
  }

  default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }

  default boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  default boolean addAll(Iterable<? extends E> c) {
    boolean modified = false;
    for (E e : c) {
      if (add(e)) {
        modified = true;
      }
    }
    return modified;
  }

  default boolean contains(Object o) {
    for (E e : this) {
      if (Objects.equals(e, o)) {
        return true;
      }
    }
    return false;
  }

  default boolean containsAll(Iterable<?> c) {
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  /** Removes all the elements and releases the underlying storage. */
  void clear();
}
