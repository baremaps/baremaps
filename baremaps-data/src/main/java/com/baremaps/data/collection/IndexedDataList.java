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

/**
 * A list of variable-size values: the values live in an {@link AppendOnlyLog} and an index maps
 * each position in the list to a position in the log. Replacing a value appends the new one and
 * leaves the old one unreferenced; the space is reclaimed only by {@link #clear()}.
 */
public class IndexedDataList<E> implements DataList<E> {

  private final DataList<Long> index;

  private final AppendOnlyLog<E> values;

  /** Creates a list in off-heap memory. */
  public IndexedDataList(DataType<E> dataType) {
    this(new MemoryAlignedDataList<>(new LongDataType()), new AppendOnlyLog<>(dataType));
  }

  public IndexedDataList(DataList<Long> index, AppendOnlyLog<E> values) {
    this.index = index;
    this.values = values;
  }

  @Override
  public long addIndexed(E value) {
    long position = values.addPositioned(value);
    return index.addIndexed(position);
  }

  @Override
  public void set(long index, E value) {
    long position = values.addPositioned(value);
    this.index.set(index, position);
  }

  @Override
  public E get(long index) {
    long position = this.index.get(index);
    return values.getPositioned(position);
  }

  @Override
  public long size() {
    return index.size();
  }

  @Override
  public void clear() {
    index.clear();
    values.clear();
  }

  @Override
  public void close() throws Exception {
    index.close();
    values.close();
  }
}
