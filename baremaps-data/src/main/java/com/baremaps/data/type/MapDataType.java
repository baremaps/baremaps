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
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DataType} for reading and writing map objects in {@link MemorySegment}s.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 */
public class MapDataType<K, V> implements DataType<Map<K, V>> {

  private final DataType<K> keyType;

  private final DataType<V> valueType;

  /**
   * Constructs a {@link MapDataType} with data types for keys and values.
   *
   * @param keyType the data type for map keys
   * @param valueType the data type for map values
   */
  public MapDataType(final DataType<K> keyType, final DataType<V> valueType) {
    this.keyType = keyType;
    this.valueType = valueType;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final Map<K, V> value) {
    int size = Integer.BYTES;
    for (Map.Entry<K, V> entry : value.entrySet()) {
      size += keyType.size(entry.getKey());
      size += valueType.size(entry.getValue());
    }
    return size;
  }

  /** {@inheritDoc} */
  @Override
  public int size(final MemorySegment segment, final long position) {
    return segment.get(JAVA_INT_UNALIGNED, position);
  }

  /** {@inheritDoc} */
  @Override
  public void write(final MemorySegment segment, final long position, final Map<K, V> value) {
    segment.set(JAVA_INT_UNALIGNED, position, size(value));
    long p = position + Integer.BYTES;
    for (Map.Entry<K, V> entry : value.entrySet()) {
      keyType.write(segment, p, entry.getKey());
      p += keyType.size(entry.getKey());
      valueType.write(segment, p, entry.getValue());
      p += valueType.size(entry.getValue());
    }
  }

  /** {@inheritDoc} */
  @Override
  public Map<K, V> read(final MemorySegment segment, final long position) {
    int size = segment.get(JAVA_INT_UNALIGNED, position);
    var map = new HashMap<K, V>(size);
    for (long p = position + Integer.BYTES; p < position + size;) {
      K key = keyType.read(segment, p);
      p += keyType.size(key);
      V value = valueType.read(segment, p);
      p += valueType.size(value);
      map.put(key, value);
    }
    return map;
  }
}
