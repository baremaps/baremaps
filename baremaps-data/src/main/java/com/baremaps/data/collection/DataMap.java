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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * A map that associates keys with values. Similar to {@link java.util.Map} but supports up to
 * {@link Long#MAX_VALUE} entries.
 *
 * @param <K> The type of keys in the map
 * @param <V> The type of values in the map
 */
public interface DataMap<K, V> extends AutoCloseable {

  /**
   * Returns the number of key-value mappings in this map.
   *
   * @return the number of key-value mappings
   */
  long size();

  /**
   * Returns the value associated with the specified key, or null if no mapping exists.
   *
   * @param key the key
   * @return the value associated with the key, or null if no mapping exists
   */
  V get(Object key);

  /**
   * Returns the values associated with the specified keys, or null for keys with no mapping.
   *
   * @param keys the keys
   * @return the values associated with the keys
   */
  default Iterable<V> getAll(Iterable<K> keys) {
    List<V> values = new ArrayList<>();
    keys.forEach(key -> values.add(get(key)));
    return values;
  }

  /**
   * Associates the specified value with the specified key in this map.
   *
   * @param key the key
   * @param value the value
   * @return the previous value associated with the key, or null if there was no mapping
   */
  V put(K key, V value);

  /**
   * Adds all entries from the specified iterable to this map.
   *
   * @param entries the entries to add
   */
  default void putAll(Iterable<Entry<K, V>> entries) {
    entries.forEach(entry -> put(entry.getKey(), entry.getValue()));
  }

  /**
   * Returns true if this map contains a mapping for the specified key.
   *
   * @param key the key to check
   * @return true if this map contains a mapping for the key
   */
  boolean containsKey(Object key);

  /**
   * Returns true if this map maps one or more keys to the specified value.
   *
   * @param value the value to check
   * @return true if this map maps one or more keys to the value
   */
  boolean containsValue(V value);

  /**
   * Removes all mappings from this map.
   */
  void clear();

  /**
   * Returns true if this map contains no mappings.
   * 
   * @return true if this map contains no mappings
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns an iterator over the keys in this map.
   *
   * @return an iterator over the keys
   */
  Iterator<K> keyIterator();

  /**
   * Returns an iterable over the keys in this map.
   *
   * @return an iterable over the keys
   */
  default Iterable<K> keys() {
    return this::keyIterator;
  }

  /**
   * Returns an iterator over the values in this map.
   *
   * @return an iterator over the values
   */
  Iterator<V> valueIterator();

  /**
   * Returns an iterable over the values in this map.
   *
   * @return an iterable over the values
   */
  default Iterable<V> values() {
    return this::valueIterator;
  }

  /**
   * Returns an iterator over the entries in this map.
   *
   * @return an iterator over the entries
   */
  Iterator<Entry<K, V>> entryIterator();

  /**
   * Returns an iterable over the entries in this map.
   *
   * @return an iterable over the entries
   */
  default Iterable<Entry<K, V>> entries() {
    return this::entryIterator;
  }

  /**
   * Performs the given action for each entry in this map.
   *
   * @param action the action to perform on each entry
   */
  default void forEach(BiConsumer<? super K, ? super V> action) {
    for (Entry<K, V> entry : entries()) {
      action.accept(entry.getKey(), entry.getValue());
    }
  }

}
