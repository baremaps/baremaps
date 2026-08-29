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

import com.baremaps.openstreetmap.utils.BatchMap;
import com.google.common.collect.Streams;
import java.util.*;

/**
 * An abstract map of data elements backed by a Postgres database.
 */
abstract class PostgresMap<K, V> implements BatchMap<K, V> {

  /** {@inheritDoc} */
  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    m.forEach(this::put);
  }

  /** {@inheritDoc} */
  @Override
  public List<V> getAll(List<K> keys) {
    return Streams.stream(keys).map(this::get).toList();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns the size of the map as a long.
   *
   * @return the size of the map
   */
  public abstract long sizeAsLong();

  /** {@inheritDoc} */
  public int size() {
    return (int) Math.min(sizeAsLong(), Integer.MAX_VALUE);
  }

  /**
   * Returns an iterator over the keys of the map.
   *
   * @return an iterator
   */
  protected abstract Iterator<K> keyIterator();

  /** {@inheritDoc} */
  @Override
  public Set<K> keySet() {
    return new KeySet();
  }

  private class KeySet extends AbstractSet<K> {
    @Override
    public Iterator<K> iterator() {
      return keyIterator();
    }

    @Override
    public int size() {
      return this.size();
    }
  }

  /**
   * Returns an iterator over the values of the map.
   *
   * @return an iterator
   */
  protected abstract Iterator<V> valueIterator();

  /** {@inheritDoc} */
  @Override
  public Collection<V> values() {
    return new ValueCollection();
  }

  private class ValueCollection extends AbstractCollection<V> {
    @Override
    public Iterator<V> iterator() {
      return valueIterator();
    }

    @Override
    public int size() {
      return PostgresMap.this.size();
    }
  }

  /**
   * Returns an iterator over the entries of the map.
   *
   * @return an iterator
   */
  protected abstract Iterator<Entry<K, V>> entryIterator();

  /** {@inheritDoc} */
  @Override
  public Set<Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public Iterator<Entry<K, V>> iterator() {
      return entryIterator();
    }

    @Override
    public int size() {
      return PostgresMap.this.size();
    }
  }
}
