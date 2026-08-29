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

import com.baremaps.data.type.DataType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map whose values live in an {@link AppendOnlyLog} and whose keys are held in a heap map
 * pointing to the positions of the values. Suited to data sets whose keys fit in the heap but whose
 * values do not. Replacing a value appends the new one and leaves the old one unreferenced; the
 * space is reclaimed only by {@link #clear()}.
 */
public class IndexedDataMap<E> implements DataMap<Long, E> {

  private final Map<Long, Long> index;

  private final AppendOnlyLog<E> values;

  /** Creates a map with a {@link HashMap} index and off-heap values. */
  public IndexedDataMap(DataType<E> dataType) {
    this(new HashMap<>(), new AppendOnlyLog<>(dataType));
  }

  public IndexedDataMap(Map<Long, Long> index, AppendOnlyLog<E> values) {
    this.index = index;
    this.values = values;
  }

  @Override
  public E put(Long key, E value) {
    var previous = index.get(key);
    var position = values.addPositioned(value);
    index.put(key, position);
    return previous == null ? null : values.getPositioned(previous);
  }

  @Override
  public E get(Object key) {
    var position = index.get(key);
    return position == null ? null : values.getPositioned(position);
  }

  @Override
  public Iterator<Long> keyIterator() {
    return index.keySet().iterator();
  }

  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return new MappingIterator<>(index.entrySet().iterator(),
        e -> Map.entry(e.getKey(), values.getPositioned(e.getValue())));
  }

  @Override
  public long size() {
    return index.size();
  }

  @Override
  public boolean containsKey(Object key) {
    return index.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return index.values().stream().map(values::getPositioned).anyMatch(value::equals);
  }

  @Override
  public void clear() {
    index.clear();
    values.clear();
  }

  @Override
  public void close() {
    values.close();
  }
}
