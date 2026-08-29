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

package com.baremaps.data.type;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DataType} for lists of values of another type: the total size in bytes, then the
 * elements back to back.
 */
public class ListDataType<T> implements DataType<List<T>> {

  private final DataType<T> dataType;

  public ListDataType(final DataType<T> dataType) {
    this.dataType = dataType;
  }

  @Override
  public int size(final List<T> values) {
    int size = Integer.BYTES;
    for (T value : values) {
      size += dataType.size(value);
    }
    return size;
  }

  @Override
  public int size(final ByteBuffer buffer, final int position) {
    return buffer.getInt(position);
  }

  @Override
  public void write(final ByteBuffer buffer, final int position, final List<T> values) {
    int p = position + Integer.BYTES;
    for (T value : values) {
      dataType.write(buffer, p, value);
      p += dataType.size(buffer, p);
    }
    buffer.putInt(position, p - position);
  }

  @Override
  public List<T> read(final ByteBuffer buffer, final int position) {
    int limit = position + buffer.getInt(position);
    var list = new ArrayList<T>();
    for (int p = position + Integer.BYTES; p < limit; p += dataType.size(buffer, p)) {
      list.add(dataType.read(buffer, p));
    }
    return list;
  }
}
