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

import static java.lang.foreign.ValueLayout.JAVA_INT_UNALIGNED;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DataType} for lists of values of another type: the total size in bytes, then the
 * elements back to back.
 */
public class ListDataType<T> implements DataType<List<T>> {

  private final DataType<T> dataType;

  // The element size when it is fixed, or 0. Lists of fixed-size elements are the common case
  // (the node ids of a way) and a fixed stride keeps their loops free of per-element size calls.
  private final int stride;

  public ListDataType(final DataType<T> dataType) {
    this.dataType = dataType;
    this.stride = dataType instanceof FixedSizeDataType<T>fixed ? fixed.size() : 0;
  }

  @Override
  public int size(final List<T> values) {
    if (stride > 0) {
      return Integer.BYTES + values.size() * stride;
    }
    int size = Integer.BYTES;
    for (T value : values) {
      size += dataType.size(value);
    }
    return size;
  }

  @Override
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  @Override
  public void write(final MemorySegment segment, final long position, final List<T> values) {
    long p = position + Integer.BYTES;
    for (T value : values) {
      dataType.write(segment, p, value);
      p += stride > 0 ? stride : dataType.size(segment, p);
    }
    segment.set(JAVA_INT_UNALIGNED, position, (int) (p - position));
  }

  @Override
  public List<T> read(final MemorySegment segment, final long position) {
    long limit = position + segment.get(JAVA_INT_UNALIGNED, position);
    if (stride > 0) {
      var list = new ArrayList<T>((int) ((limit - position - Integer.BYTES) / stride));
      for (long p = position + Integer.BYTES; p < limit; p += stride) {
        list.add(dataType.read(segment, p));
      }
      return list;
    }
    var list = new ArrayList<T>();
    for (long p = position + Integer.BYTES; p < limit; p += dataType.size(segment, p)) {
      list.add(dataType.read(segment, p));
    }
    return list;
  }
}
