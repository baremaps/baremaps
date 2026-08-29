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

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Views between the data collections and the {@code java.util} collections. The views are live and
 * unwrapping a view returns the original; sizes above {@link Integer#MAX_VALUE} are truncated by
 * the {@code java.util} views.
 */
public class DataConversions {

  private DataConversions() {
    // Utility class
  }

  public static <E> Collection<E> asCollection(DataCollection<E> dataCollection) {
    if (dataCollection instanceof DataCollectionAdapter<E>adapter) {
      return adapter.collection;
    }
    return new CollectionAdapter<>(dataCollection);
  }

  public static <E> DataCollection<E> asDataCollection(Collection<E> collection) {
    if (collection instanceof CollectionAdapter<E>adapter) {
      return adapter.collection;
    }
    return new DataCollectionAdapter<>(collection);
  }

  public static <E> List<E> asList(DataList<E> dataList) {
    if (dataList instanceof DataListAdapter<E>adapter) {
      return adapter.list;
    }
    return new ListAdapter<>(dataList);
  }

  public static <E> DataList<E> asDataList(List<E> list) {
    if (list instanceof ListAdapter<E>adapter) {
      return adapter.list;
    }
    return new DataListAdapter<>(list);
  }

  /** Returns a map view; closing it closes the data map. */
  public static <K, V> CloseableMap<K, V> asMap(DataMap<K, V> dataMap) {
    if (dataMap instanceof DataMapAdapter<K, V>adapter
        && adapter.map instanceof CloseableMap<K, V>map) {
      return map;
    }
    return new MapAdapter<>(dataMap);
  }

  public static <K, V> DataMap<K, V> asDataMap(Map<K, V> map) {
    if (map instanceof MapAdapter<K, V>adapter) {
      return adapter.map;
    }
    return new DataMapAdapter<>(map);
  }

  private static int truncate(long size) {
    return (int) Math.min(size, Integer.MAX_VALUE);
  }

  private static void closeIfCloseable(Object object) throws Exception {
    if (object instanceof AutoCloseable closeable) {
      closeable.close();
    }
  }

  private static class CollectionAdapter<E> extends AbstractCollection<E> {

    private final DataCollection<E> collection;

    CollectionAdapter(DataCollection<E> collection) {
      this.collection = collection;
    }

    @Override
    public int size() {
      return truncate(collection.size());
    }

    @Override
    public boolean add(E value) {
      return collection.add(value);
    }

    @Override
    public void clear() {
      collection.clear();
    }

    @Override
    public Iterator<E> iterator() {
      return collection.iterator();
    }
  }

  private static class DataCollectionAdapter<E> implements DataCollection<E> {

    private final Collection<E> collection;

    DataCollectionAdapter(Collection<E> collection) {
      this.collection = collection;
    }

    @Override
    public long size() {
      return collection.size();
    }

    @Override
    public boolean add(E value) {
      return collection.add(value);
    }

    @Override
    public void clear() {
      collection.clear();
    }

    @Override
    public Iterator<E> iterator() {
      return collection.iterator();
    }

    @Override
    public void close() throws Exception {
      closeIfCloseable(collection);
    }
  }

  private static class ListAdapter<E> extends AbstractList<E> {

    private final DataList<E> list;

    ListAdapter(DataList<E> list) {
      this.list = list;
    }

    @Override
    public boolean add(E value) {
      return list.add(value);
    }

    @Override
    public E set(int index, E value) {
      var oldValue = list.get(index);
      list.set(index, value);
      return oldValue;
    }

    @Override
    public E get(int index) {
      return list.get(index);
    }

    @Override
    public int size() {
      return truncate(list.size());
    }

    @Override
    public void clear() {
      list.clear();
    }
  }

  private static class DataListAdapter<E> implements DataList<E> {

    private final List<E> list;

    DataListAdapter(List<E> list) {
      this.list = list;
    }

    @Override
    public long size() {
      return list.size();
    }

    @Override
    public void clear() {
      list.clear();
    }

    @Override
    public long addIndexed(E value) {
      list.add(value);
      return list.size() - 1L;
    }

    @Override
    public void set(long index, E value) {
      list.set(Math.toIntExact(index), value);
    }

    @Override
    public E get(long index) {
      return list.get(Math.toIntExact(index));
    }

    @Override
    public void close() throws Exception {
      closeIfCloseable(list);
    }
  }

  private static class MapAdapter<K, V> extends AbstractMap<K, V> implements CloseableMap<K, V> {

    private final DataMap<K, V> map;

    MapAdapter(DataMap<K, V> map) {
      this.map = map;
    }

    @Override
    public void close() throws Exception {
      map.close();
    }

    @Override
    public V put(K key, V value) {
      return map.put(key, value);
    }

    @Override
    public V get(Object key) {
      return map.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
      return map.containsKey(key);
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
      return new AbstractSet<>() {
        @Override
        public Iterator<Entry<K, V>> iterator() {
          return map.entryIterator();
        }

        @Override
        public int size() {
          return truncate(map.size());
        }
      };
    }
  }

  private static class DataMapAdapter<K, V> implements DataMap<K, V> {

    private final Map<K, V> map;

    DataMapAdapter(Map<K, V> map) {
      this.map = map;
    }

    @Override
    public long size() {
      return map.size();
    }

    @Override
    public V get(Object key) {
      return map.get(key);
    }

    @Override
    public V put(K key, V value) {
      return map.put(key, value);
    }

    @Override
    public boolean containsKey(Object key) {
      return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return map.containsValue(value);
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public Iterator<K> keyIterator() {
      return map.keySet().iterator();
    }

    @Override
    public Iterator<V> valueIterator() {
      return map.values().iterator();
    }

    @Override
    public Iterator<Entry<K, V>> entryIterator() {
      return map.entrySet().iterator();
    }

    @Override
    public void close() throws Exception {
      closeIfCloseable(map);
    }
  }
}
