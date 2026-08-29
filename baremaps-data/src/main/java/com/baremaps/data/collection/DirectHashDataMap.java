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

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;
import com.baremaps.data.type.LongDataType;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An open-addressing hash map of fixed capacity from {@code long} keys to fixed-size values, for
 * keys that are neither dense nor sorted. Keys and values live in two parallel tables; collisions
 * are resolved by linear probing, which keeps probes within a few cache lines.
 *
 * <p>
 * Two key values are reserved as markers for empty slots and would be rejected by
 * {@link #put(Long, Object)}. The map does not support removal, so it has no tombstones.
 */
public class DirectHashDataMap<V> implements DataMap<Long, V> {

  private static final long EMPTY_KEY = Long.MIN_VALUE;

  private static final LongDataType KEY_TYPE = new LongDataType();

  private final FixedSizeDataType<V> valueType;

  private final Memory keyMemory;

  private final Memory valueMemory;

  private final long capacity;

  private long size;

  /** Creates a map of the given capacity in off-heap memory. */
  public DirectHashDataMap(FixedSizeDataType<V> valueType, long capacity) {
    this(valueType, capacity, Memory.offHeap(), Memory.offHeap());
  }

  public DirectHashDataMap(FixedSizeDataType<V> valueType, long capacity, Memory keyMemory,
      Memory valueMemory) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity must be greater than zero");
    }
    if (keyMemory.segmentSize() % KEY_TYPE.size() != 0
        || valueMemory.segmentSize() % valueType.size() != 0) {
      throw new DataCollectionException("The segment sizes must be multiples of the value sizes");
    }
    this.valueType = valueType;
    this.keyMemory = keyMemory;
    this.valueMemory = valueMemory;
    this.capacity = capacity;
    this.size = 0;
    for (long slot = 0; slot < capacity; slot++) {
      storeKey(slot, EMPTY_KEY);
    }
  }

  private long hash(long key) {
    // Fibonacci hashing spreads consecutive keys, which are common, over the table.
    return Long.remainderUnsigned((key * 0x9E3779B97F4A7C15L) >>> 16, capacity);
  }

  /** Returns the slot holding the key, or the empty slot where it would go, or -1 if full. */
  private long findSlot(long key) {
    long slot = hash(key);
    for (long i = 0; i < capacity; i++) {
      long current = readKey(slot);
      if (current == EMPTY_KEY || current == key) {
        return slot;
      }
      slot = slot + 1 == capacity ? 0 : slot + 1;
    }
    return -1;
  }

  private long readKey(long slot) {
    return keyMemory.read(KEY_TYPE, slot * KEY_TYPE.size());
  }

  private void storeKey(long slot, long key) {
    keyMemory.write(KEY_TYPE, slot * KEY_TYPE.size(), key);
  }

  private V readValue(long slot) {
    return valueMemory.read(valueType, slot * valueType.size());
  }

  private void storeValue(long slot, V value) {
    valueMemory.write(valueType, slot * valueType.size(), value);
  }

  @Override
  public V put(Long key, V value) {
    Objects.requireNonNull(key, "Key cannot be null");
    Objects.requireNonNull(value, "Value cannot be null");
    if (key == EMPTY_KEY) {
      throw new IllegalArgumentException("Reserved key value: " + key);
    }
    long slot = findSlot(key);
    if (slot == -1) {
      throw new IllegalStateException("Map is full");
    }
    V previous = null;
    if (readKey(slot) == key) {
      previous = readValue(slot);
    } else {
      storeKey(slot, key);
      size++;
    }
    storeValue(slot, value);
    return previous;
  }

  private long slotOf(Object keyObject) {
    if (!(keyObject instanceof Long key) || key == EMPTY_KEY) {
      return -1;
    }
    long slot = findSlot(key);
    return slot >= 0 && readKey(slot) == key ? slot : -1;
  }

  @Override
  public V get(Object key) {
    long slot = slotOf(key);
    return slot < 0 ? null : readValue(slot);
  }

  @Override
  public boolean containsKey(Object key) {
    return slotOf(key) >= 0;
  }

  @Override
  public boolean containsValue(Object value) {
    Iterator<V> iterator = valueIterator();
    while (iterator.hasNext()) {
      if (iterator.next().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public long size() {
    return size;
  }

  @Override
  public void clear() {
    for (long slot = 0; slot < capacity; slot++) {
      storeKey(slot, EMPTY_KEY);
    }
    size = 0;
  }

  @Override
  public Iterator<Entry<Long, V>> entryIterator() {
    return new Iterator<>() {
      private long slot = 0;
      private long returned = 0;

      @Override
      public boolean hasNext() {
        return returned < size;
      }

      @Override
      public Entry<Long, V> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        while (readKey(slot) == EMPTY_KEY) {
          slot++;
        }
        long key = readKey(slot);
        V value = readValue(slot);
        slot++;
        returned++;
        return Map.entry(key, value);
      }
    };
  }

  @Override
  public void close() {
    keyMemory.close();
    valueMemory.close();
  }
}
