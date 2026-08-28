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
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A map that uses direct hashing with open addressing and linear probing. Provides O(1) access time
 * for both get and put operations with a fixed capacity.
 * 
 * @param <V> the type of values in the map
 */
public class DirectHashDataMap<V> implements DataMap<Long, V> {

  private static final long EMPTY_KEY = Long.MIN_VALUE;
  private static final long TOMBSTONE = Long.MIN_VALUE + 1;

  private final FixedSizeDataType<V> dataType;
  private final FixedSizeDataType<Long> keyType;
  private final Memory<?> keyMemory;
  private final Memory<?> valueMemory;

  private final long capacity;
  private final int valueSize;
  private final int keySize;

  private long size;

  /**
   * Creates a new builder for a DirectHashDataMap.
   *
   * @param <V> the type of values
   * @return a new builder
   */
  public static <V> Builder<V> builder() {
    return new Builder<>();
  }

  /**
   * Builder for DirectHashDataMap.
   *
   * @param <V> the type of values
   */
  public static class Builder<V> {
    private FixedSizeDataType<Long> keyType;
    private FixedSizeDataType<V> dataType;
    private Memory<?> keyMemory;
    private Memory<?> valueMemory;
    private long capacity;

    /**
     * Sets the key type for the map.
     *
     * @param keyType the data type for keys
     * @return this builder
     */
    public Builder<V> keyType(FixedSizeDataType<Long> keyType) {
      this.keyType = keyType;
      return this;
    }

    /**
     * Sets the data type for the map.
     *
     * @param dataType the data type for values
     * @return this builder
     */
    public Builder<V> dataType(FixedSizeDataType<V> dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Sets the key memory for the map.
     *
     * @param keyMemory the memory for keys
     * @return this builder
     */
    public Builder<V> keyMemory(Memory<?> keyMemory) {
      this.keyMemory = keyMemory;
      return this;
    }

    /**
     * Sets the value memory for the map.
     *
     * @param valueMemory the memory for values
     * @return this builder
     */
    public Builder<V> valueMemory(Memory<?> valueMemory) {
      this.valueMemory = valueMemory;
      return this;
    }

    /**
     * Sets the capacity for the map.
     *
     * @param capacity the fixed capacity of the map
     * @return this builder
     */
    public Builder<V> capacity(long capacity) {
      this.capacity = capacity;
      return this;
    }

    /**
     * Builds a new DirectHashDataMap.
     *
     * @return a new DirectHashDataMap
     * @throws IllegalStateException if any required parameter is missing
     */
    public DirectHashDataMap<V> build() {
      if (keyType == null) {
        throw new IllegalStateException("Key type must be specified");
      }
      if (dataType == null) {
        throw new IllegalStateException("Data type must be specified");
      }
      if (keyMemory == null) {
        throw new IllegalStateException("Key memory must be specified");
      }
      if (valueMemory == null) {
        throw new IllegalStateException("Value memory must be specified");
      }
      if (capacity <= 0) {
        throw new IllegalStateException("Capacity must be greater than zero");
      }

      return new DirectHashDataMap<>(keyType, dataType, keyMemory, valueMemory, capacity);
    }
  }

  /**
   * Constructs a DirectHashDataMap with the specified capacity.
   *
   * @param keyType the data type for keys
   * @param dataType the data type for values
   * @param keyMemory the memory for keys
   * @param valueMemory the memory for values
   * @param capacity the fixed capacity of the map
   */
  private DirectHashDataMap(
      FixedSizeDataType<Long> keyType,
      FixedSizeDataType<V> dataType,
      Memory<?> keyMemory,
      Memory<?> valueMemory,
      long capacity) {
    this.keyType = keyType;
    this.dataType = dataType;
    this.keyMemory = keyMemory;
    this.valueMemory = valueMemory;
    this.capacity = capacity;
    this.keySize = keyType.size();
    this.valueSize = dataType.size();
    this.size = 0;

    // Initialize all keys to EMPTY_KEY
    for (long i = 0; i < capacity; i++) {
      storeKey(i, EMPTY_KEY);
    }
  }

  /**
   * Computes the hash value for a key.
   * 
   * @param key the key to hash
   * @return the hash value
   */
  private long hash(long key) {
    // Using a variant of the Knuth multiplicative method
    // with the golden ratio
    final long GOLDEN_RATIO = 0x9E3779B97F4A7C15L;
    return ((key * GOLDEN_RATIO) >>> 16) % capacity;
  }

  /**
   * Finds the slot for a key, either for retrieval or insertion.
   * 
   * @param key the key to find
   * @param forInsertion whether we're finding for insertion or retrieval
   * @return the slot index or -1 if not found and not for insertion
   */
  private long findSlot(long key, boolean forInsertion) {
    long index = hash(key);
    long tombstoneSlot = -1;

    // Linear probing to handle collisions
    for (long i = 0; i < capacity; i++) {
      long currentIndex = (index + i) % capacity;
      long currentKey = readKey(currentIndex);

      if (currentKey == EMPTY_KEY) {
        // Found empty slot
        return forInsertion ? (tombstoneSlot != -1 ? tombstoneSlot : currentIndex) : -1;
      } else if (currentKey == TOMBSTONE) {
        // Mark first tombstone for possible reuse
        if (tombstoneSlot == -1) {
          tombstoneSlot = currentIndex;
        }
      } else if (currentKey == key) {
        // Found the key
        return currentIndex;
      }
    }

    // If we're inserting and found a tombstone earlier, use that
    if (forInsertion && tombstoneSlot != -1) {
      return tombstoneSlot;
    }

    // Map is full or key not found
    return -1;
  }

  /**
   * Reads a key from the specified slot.
   * 
   * @param slot the slot index
   * @return the key at the slot
   */
  private long readKey(long slot) {
    long position = slot * keySize;
    int segmentIndex = (int) (position >>> keyMemory.segmentShift());
    int segmentOffset = (int) (position & keyMemory.segmentMask());
    ByteBuffer segment = keyMemory.segment(segmentIndex);
    return keyType.read(segment, segmentOffset);
  }

  /**
   * Stores a key at the specified slot.
   * 
   * @param slot the slot index
   * @param key the key to store
   */
  private void storeKey(long slot, long key) {
    long position = slot * keySize;
    int segmentIndex = (int) (position >>> keyMemory.segmentShift());
    int segmentOffset = (int) (position & keyMemory.segmentMask());
    ByteBuffer segment = keyMemory.segment(segmentIndex);
    keyType.write(segment, segmentOffset, key);
  }

  /**
   * Reads a value from the specified slot.
   * 
   * @param slot the slot index
   * @return the value at the slot
   */
  private V readValue(long slot) {
    long position = slot * valueSize;
    int segmentIndex = (int) (position >>> valueMemory.segmentShift());
    int segmentOffset = (int) (position & valueMemory.segmentMask());
    ByteBuffer segment = valueMemory.segment(segmentIndex);
    return dataType.read(segment, segmentOffset);
  }

  /**
   * Stores a value at the specified slot.
   * 
   * @param slot the slot index
   * @param value the value to store
   */
  private void storeValue(long slot, V value) {
    long position = slot * valueSize;
    int segmentIndex = (int) (position >>> valueMemory.segmentShift());
    int segmentOffset = (int) (position & valueMemory.segmentMask());
    ByteBuffer segment = valueMemory.segment(segmentIndex);
    dataType.write(segment, segmentOffset, value);
  }

  /** {@inheritDoc} */
  @Override
  public V put(Long key, V value) {
    Objects.requireNonNull(key, "Key cannot be null");
    Objects.requireNonNull(value, "Value cannot be null");

    if (key == EMPTY_KEY || key == TOMBSTONE) {
      throw new IllegalArgumentException("Reserved key value: " + key);
    }

    long slot = findSlot(key, true);
    if (slot == -1) {
      throw new IllegalStateException("Map is full");
    }

    long existingKey = readKey(slot);
    V previousValue = null;

    if (existingKey != EMPTY_KEY && existingKey != TOMBSTONE) {
      // Slot contains an existing key
      previousValue = readValue(slot);
    } else {
      // New entry
      size++;
    }

    storeKey(slot, key);
    storeValue(slot, value);

    return previousValue;
  }

  /** {@inheritDoc} */
  @Override
  public V get(Object keyObj) {
    if (!(keyObj instanceof Long key)) {
      return null;
    }

    if (key == EMPTY_KEY || key == TOMBSTONE) {
      return null;
    }

    long slot = findSlot(key, false);
    if (slot == -1) {
      return null;
    }

    return readValue(slot);
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(Object keyObj) {
    if (!(keyObj instanceof Long key)) {
      return false;
    }

    if (key == EMPTY_KEY || key == TOMBSTONE) {
      return false;
    }

    return findSlot(key, false) != -1;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      return false;
    }

    Iterator<V> iterator = valueIterator();
    while (iterator.hasNext()) {
      if (value.equals(iterator.next())) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public long size() {
    return size;
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    for (long i = 0; i < capacity; i++) {
      storeKey(i, EMPTY_KEY);
    }
    size = 0;
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Long> keyIterator() {
    return new Iterator<>() {
      private long currentIndex = 0;
      private long returnedCount = 0;

      @Override
      public boolean hasNext() {
        return returnedCount < size;
      }

      @Override
      public Long next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        while (currentIndex < capacity) {
          long key = readKey(currentIndex++);
          if (key != EMPTY_KEY && key != TOMBSTONE) {
            returnedCount++;
            return key;
          }
        }

        throw new NoSuchElementException();
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<V> valueIterator() {
    return new Iterator<>() {
      private long currentIndex = 0;
      private long returnedCount = 0;

      @Override
      public boolean hasNext() {
        return returnedCount < size;
      }

      @Override
      public V next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        while (currentIndex < capacity) {
          long key = readKey(currentIndex);
          if (key != EMPTY_KEY && key != TOMBSTONE) {
            returnedCount++;
            return readValue(currentIndex++);
          }
          currentIndex++;
        }

        throw new NoSuchElementException();
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Entry<Long, V>> entryIterator() {
    return new Iterator<>() {
      private long currentIndex = 0;
      private long returnedCount = 0;

      @Override
      public boolean hasNext() {
        return returnedCount < size;
      }

      @Override
      public Entry<Long, V> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        while (currentIndex < capacity) {
          long key = readKey(currentIndex);
          if (key != EMPTY_KEY && key != TOMBSTONE) {
            V value = readValue(currentIndex);
            currentIndex++;
            returnedCount++;
            return Map.entry(key, value);
          }
          currentIndex++;
        }

        throw new NoSuchElementException();
      }
    };
  }

  @Override
  public void close() throws Exception {
    try {
      keyMemory.close();
      valueMemory.close();
    } catch (Exception e) {
      throw new DataCollectionException(e);
    }
  }
}
