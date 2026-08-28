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
 * Provides an interface to a repository.
 *
 * @param <K> The type of the keys
 * @param <V> The type of the values
 */
public interface Repository<K, V> {

  /**
   * Creates the repository.
   *
   * @throws RepositoryException
   */
  void create() throws RepositoryException;

  /**
   * Drops the repository.
   *
   * @throws RepositoryException
   */
  void drop() throws RepositoryException;

  /**
   * Truncate the repository.
   *
   * @throws RepositoryException
   */
  void truncate() throws RepositoryException;

  /**
   * Gets a value by its key.
   *
   * @param key the id of the value
   * @return the selected value if it exists, null otherwise
   * @throws RepositoryException
   */
  V get(K key) throws RepositoryException;

  /**
   * Gets a list of values by their keys.
   *
   * @param keys a list of keys
   * @return a list of values
   * @throws RepositoryException
   */
  List<V> get(List<K> keys) throws RepositoryException;

  /**
   * Puts a new value into the repository.
   *
   * @param value the value to put
   * @throws RepositoryException
   */
  void put(V value) throws RepositoryException;

  /**
   * Puts a list of values into the repository.
   *
   * @param values a list of the values to put
   * @throws RepositoryException
   */
  void put(List<V> values) throws RepositoryException;

  /**
   * Deletes a value by key.
   *
   * @param key the key of the value to delete
   * @throws RepositoryException
   */
  void delete(K key) throws RepositoryException;

  /**
   * Deletes a list of values in the repository.
   *
   * @param keys the list of keys
   * @throws RepositoryException
   */
  void delete(List<K> keys) throws RepositoryException;

  /**
   * Imports the given values into the repository using a fast copy interface.
   *
   * @param values a list of the values to add
   * @throws RepositoryException If an exception occurs while copying
   */
  void copy(List<V> values) throws RepositoryException;
}
