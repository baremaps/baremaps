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
import com.baremaps.data.type.LongDataType;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map of variable-size values: the values live in an {@link AppendOnlyLog} and a map of
 * fixed-size positions, such as a {@link DenseDataMap}, finds them. Replacing a value appends the
 * new one and leaves the old one unreferenced; the space is reclaimed only by {@link #clear()}.
 *
 * <p>
 * Thread safety is that of the position map, as log appends may be concurrent.
 */
public class VariableSizeDataMap<E> implements DataMap<Long, E> {

  private final DataMap<Long, Long> positions;

  private final AppendOnlyLog<E> values;

  /** Creates a map in off-heap memory, with a {@link DenseDataMap} for the positions. */
  public VariableSizeDataMap(DataType<E> dataType) {
    this(new DenseDataMap<>(new LongDataType()), new AppendOnlyLog<>(dataType));
  }

  public VariableSizeDataMap(DataMap<Long, Long> positions, AppendOnlyLog<E> values) {
    this.positions = positions;
    this.values = values;
  }

  @Override
  public E put(Long key, E value) {
    long position = values.addPositioned(value);
    Long previous = positions.put(key, position);
    return previous == null ? null : values.getPositioned(previous);
  }

  @Override
  public E get(Object key) {
    Long position = positions.get(key);
    return position == null ? null : values.getPositioned(position);
  }

  @Override
  public boolean containsKey(Object key) {
    return positions.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    Iterator<E> iterator = valueIterator();
    while (iterator.hasNext()) {
      if (iterator.next().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public long size() {
    return positions.size();
  }

  @Override
  public void clear() {
    positions.clear();
    values.clear();
  }

  @Override
  public Iterator<Long> keyIterator() {
    return positions.keyIterator();
  }

  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return new MappingIterator<>(positions.entryIterator(),
        entry -> Map.entry(entry.getKey(), values.getPositioned(entry.getValue())));
  }

  @Override
  public void close() throws Exception {
    positions.close();
    values.close();
  }
}
