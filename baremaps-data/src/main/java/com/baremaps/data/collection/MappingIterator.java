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
import java.util.function.Function;

/** An iterator that applies a function to the elements of another iterator. */
final class MappingIterator<S, T> implements Iterator<T> {

  private final Iterator<S> source;

  private final Function<? super S, ? extends T> mapper;

  MappingIterator(Iterator<S> source, Function<? super S, ? extends T> mapper) {
    this.source = source;
    this.mapper = mapper;
  }

  @Override
  public boolean hasNext() {
    return source.hasNext();
  }

  @Override
  public T next() {
    return mapper.apply(source.next());
  }
}
