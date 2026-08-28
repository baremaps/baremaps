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

/**
 * A decorator that transforms elements of a data collection using a function.
 *
 * @param <S> The source element type
 * @param <T> The transformed element type
 */
public class DataCollectionMapper<S, T> implements DataCollection<T> {

  private final DataCollection<S> collection;

  private final Function<S, T> mapper;

  /**
   * Constructs a new mapper for a data collection.
   *
   * @param collection the source collection
   * @param mapper the function to transform elements
   */
  public DataCollectionMapper(DataCollection<S> collection, Function<S, T> mapper) {
    this.collection = collection;
    this.mapper = mapper;
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return collection.size();
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<T> iterator() {
    return collection.stream().map(this.mapper).iterator();
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    collection.clear();
  }

  @Override
  public void close() throws Exception {
    collection.close();
  }
}
