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

package com.baremaps.postgres.openstreetmap;

import java.util.List;

/**
 * Reads and writes the values of a repository.
 *
 * <p>
 * Creating and dropping the tables is the business of {@link OsmSchema}, so that this interface
 * says one thing: how entities go in and come out.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 */
public interface Repository<K, V> {

  /**
   * Gets a value by its key.
   *
   * @param key the id of the value
   * @return the selected value if it exists, null otherwise
   */
  V get(K key) throws RepositoryException;

  /**
   * Gets a list of values by their keys.
   *
   * @param keys a list of keys
   * @return the value of each key, in the order asked for, with null for the absent ones
   */
  List<V> get(List<K> keys) throws RepositoryException;

  /**
   * Puts a value into the repository, replacing the one it already holds under that key.
   *
   * @param value the value to put
   */
  void put(V value) throws RepositoryException;

  /**
   * Puts a list of values into the repository, replacing the ones it already holds under those
   * keys.
   *
   * @param values a list of the values to put
   */
  void put(List<V> values) throws RepositoryException;

  /**
   * Deletes a value by key.
   *
   * @param key the key of the value to delete
   */
  void delete(K key) throws RepositoryException;

  /**
   * Deletes a list of values in the repository.
   *
   * @param keys the list of keys
   */
  void delete(List<K> keys) throws RepositoryException;

  /**
   * Imports the given values using the binary copy interface, which is faster than {@link #put} but
   * does not replace the rows it collides with.
   *
   * @param values a list of the values to add
   */
  void copy(List<V> values) throws RepositoryException;
}
