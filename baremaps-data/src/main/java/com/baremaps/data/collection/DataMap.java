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
 * A map that can hold more than {@link Integer#MAX_VALUE} entries, typically outside the heap.
 * Values are never null, so {@code get} returning null means the key is absent.
 */
public interface DataMap<K, V> extends AutoCloseable {

  long size();

  default boolean isEmpty() {
    return size() == 0;
  }

  V get(Object key);

  default Iterable<V> getAll(Iterable<K> keys) {
    List<V> values = new ArrayList<>();
    keys.forEach(key -> values.add(get(key)));
    return values;
  }

  /** Associates a value with a key and returns the previous value, if any and if known. */
  V put(K key, V value);

  default void putAll(Iterable<Entry<K, V>> entries) {
    entries.forEach(entry -> put(entry.getKey(), entry.getValue()));
  }

  boolean containsKey(Object key);

  boolean containsValue(Object value);

  /** Removes all the entries and releases the underlying storage. */
  void clear();

  Iterator<Entry<K, V>> entryIterator();

  default Iterator<K> keyIterator() {
    return new MappingIterator<>(entryIterator(), Entry::getKey);
  }

  default Iterator<V> valueIterator() {
    return new MappingIterator<>(entryIterator(), Entry::getValue);
  }

  default Iterable<K> keys() {
    return this::keyIterator;
  }

  default Iterable<V> values() {
    return this::valueIterator;
  }

  default Iterable<Entry<K, V>> entries() {
    return this::entryIterator;
  }

  default void forEach(BiConsumer<? super K, ? super V> action) {
    for (Entry<K, V> entry : entries()) {
      action.accept(entry.getKey(), entry.getValue());
    }
  }
}
