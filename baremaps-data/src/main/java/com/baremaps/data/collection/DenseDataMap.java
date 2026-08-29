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

import static java.lang.foreign.ValueLayout.JAVA_LONG_UNALIGNED;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.FixedSizeDataType;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A map from {@code long} keys to fixed-size values, laid out as a flat array of the key space that
 * is split in pages allocated on first write. It is the map of choice for the ids of a planet-sized
 * OpenStreetMap import, which are dense (a large majority of the ids below the maximum exist) but
 * not contiguous.
 *
 * <p>
 * A key is split into a page number and a slot. A page table maps the page number to the ordinal of
 * its page, or 0 while no key of that page has been written; the values of a page are contiguous,
 * so the position of a slot is a shift. A presence bitmap, one bit per slot, tells written keys
 * from the zero-filled rest of a page, which is what lets {@link #get(Object)} return {@code null}
 * for an absent key. A lookup therefore costs three dependent reads and no search, and the space
 * per key is the value plus one bit, plus the unused slots of the pages that have been touched:
 * sparse keys waste up to a page each, so a data set whose ids are spread thin is better served by
 * {@link SparseDataMap}.
 *
 * <p>
 * The page count and the size are persisted in the header of the page-table memory by
 * {@link #close()}; reopen a map with the same page size and data type.
 *
 * <p>
 * Thread safety: any number of threads may put and get concurrently. Pages are allocated under a
 * lock, presence bits are set atomically, and a value write is a plain write, so a read of a key is
 * consistent once the put that wrote it has returned. Two concurrent puts of the same key leave one
 * of the two values.
 */
public class DenseDataMap<E> implements DataMap<Long, E> {

  /** The default number of keys per page, as a power of two. */
  public static final int DEFAULT_PAGE_SHIFT = 10;

  private static final int PAGE_COUNT_OFFSET = 0;

  private static final int SIZE_OFFSET = Long.BYTES;

  private static final int TABLE_LIMIT_OFFSET = 2 * Long.BYTES;

  private final FixedSizeDataType<E> dataType;

  private final Memory table;

  private final Memory presence;

  private final Memory values;

  private final int pageShift;

  private final long slotMask;

  private final int valueShift;

  // Bytes per page in the values and presence memories, as shifts.
  private final int pageBytesShift;

  private final int presenceBytesShift;

  // Keys whose page number would not fit in the table memory are rejected.
  private final long maxKey;

  private final AtomicLong size;

  // The number of pages allocated so far and the number of page-table entries that may be
  // non-zero, both guarded by "this" for writers. Reads beyond the limit never touch the table,
  // which keeps a miss from mapping a segment.
  private volatile long pageCount;

  private volatile long tableLimit;

  /** Creates a map in off-heap memory with pages of {@code 2^DEFAULT_PAGE_SHIFT} keys. */
  public DenseDataMap(FixedSizeDataType<E> dataType) {
    this(dataType, DEFAULT_PAGE_SHIFT, Memory.offHeap(), Memory.offHeap(), Memory.offHeap());
  }

  /**
   * Creates or reopens a map over the given memories.
   *
   * @param dataType the type of the values, whose size must be a power of two
   * @param pageShift the number of keys per page, as a power of two, at least 6
   * @param table the memory of the page table
   * @param presence the memory of the presence bitmaps
   * @param values the memory of the values
   */
  public DenseDataMap(FixedSizeDataType<E> dataType, int pageShift, Memory table,
      Memory presence, Memory values) {
    int valueSize = dataType.size();
    if (valueSize <= 0 || (valueSize & -valueSize) != valueSize) {
      throw new DataCollectionException("The data type size must be a power of 2");
    }
    if (pageShift < 6 || pageShift > 30) {
      throw new DataCollectionException("The page shift must be between 6 and 30");
    }
    this.dataType = dataType;
    this.table = table;
    this.presence = presence;
    this.values = values;
    this.pageShift = pageShift;
    this.slotMask = (1L << pageShift) - 1;
    this.valueShift = Integer.numberOfTrailingZeros(valueSize);
    this.pageBytesShift = pageShift + valueShift;
    this.presenceBytesShift = pageShift - 3;
    if (1L << pageBytesShift > values.segmentSize()
        || 1L << presenceBytesShift > presence.segmentSize()) {
      throw new DataCollectionException("The segment size is too small for a page");
    }
    // The table holds a long per page and its segments are indexed by an int.
    int tableShift = Long.numberOfTrailingZeros(table.segmentSize());
    this.maxKey = (1L << Math.min(62, Integer.SIZE - 1 + tableShift - 3 + pageShift)) - 1;
    MemorySegment header = table.header();
    this.pageCount = header.get(JAVA_LONG_UNALIGNED, PAGE_COUNT_OFFSET);
    this.size = new AtomicLong(header.get(JAVA_LONG_UNALIGNED, SIZE_OFFSET));
    this.tableLimit = header.get(JAVA_LONG_UNALIGNED, TABLE_LIMIT_OFFSET);
  }

  /** Returns the ordinal of the page of a key, or -1 if none has been allocated. */
  private long pageOf(long key) {
    long pageNumber = key >>> pageShift;
    if (pageNumber >= tableLimit) {
      return -1;
    }
    return table.get(JAVA_LONG_UNALIGNED, pageNumber << 3) - 1;
  }

  private synchronized long allocatePage(long key) {
    long pageNumber = key >>> pageShift;
    long page = table.get(JAVA_LONG_UNALIGNED, pageNumber << 3) - 1;
    if (page < 0) {
      page = pageCount;
      pageCount = page + 1;
      table.set(JAVA_LONG_UNALIGNED, pageNumber << 3, page + 1);
      // Published after the entry, so a reader below the limit sees the entry.
      tableLimit = Math.max(tableLimit, pageNumber + 1);
    }
    return page;
  }

  private long presencePosition(long page, long slot) {
    return (page << presenceBytesShift) + ((slot >>> 6) << 3);
  }

  private boolean isPresent(long page, long slot) {
    long bits = presence.get(JAVA_LONG_UNALIGNED, presencePosition(page, slot));
    return (bits >>> (slot & 63) & 1L) != 0;
  }

  private long valuePosition(long page, long slot) {
    return (page << pageBytesShift) + (slot << valueShift);
  }

  private long checkKey(Long key) {
    Objects.requireNonNull(key, "Key cannot be null");
    if (key < 0 || key > maxKey) {
      throw new IndexOutOfBoundsException(
          "Key should be between 0 and " + maxKey + ", but was " + key);
    }
    return key;
  }

  @Override
  public E put(Long key, E value) {
    Objects.requireNonNull(value, "Value cannot be null");
    long k = checkKey(key);
    long page = pageOf(k);
    if (page < 0) {
      page = allocatePage(k);
    }
    long slot = k & slotMask;
    long valuePosition = valuePosition(page, slot);
    long bit = 1L << (slot & 63);
    E previous = null;
    if (isPresent(page, slot)) {
      previous = values.read(dataType, valuePosition);
      values.write(dataType, valuePosition, value);
    } else {
      values.write(dataType, valuePosition, value);
      if ((presence.getAndSetBits(presencePosition(page, slot), bit) & bit) == 0) {
        size.incrementAndGet();
      }
    }
    return previous;
  }

  @Override
  public E get(Object keyObject) {
    if (!(keyObject instanceof Long key) || key < 0 || key > maxKey) {
      return null;
    }
    long page = pageOf(key);
    if (page < 0) {
      return null;
    }
    long slot = key & slotMask;
    return isPresent(page, slot) ? values.read(dataType, valuePosition(page, slot)) : null;
  }

  @Override
  public boolean containsKey(Object keyObject) {
    if (!(keyObject instanceof Long key) || key < 0 || key > maxKey) {
      return false;
    }
    long page = pageOf(key);
    return page >= 0 && isPresent(page, key & slotMask);
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
    return size.get();
  }

  /**
   * Returns the number of pages allocated so far. Compared with {@link #size()}, it tells how much
   * of the allocated space the keys fill, which is what decides whether this map or a
   * {@link SparseDataMap} suits a data set.
   */
  public long pageCount() {
    return pageCount;
  }

  @Override
  public synchronized void clear() {
    try {
      table.clear();
      presence.clear();
      values.clear();
      pageCount = 0;
      tableLimit = 0;
      size.set(0);
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  /** Iterates the entries in increasing key order. */
  @Override
  public Iterator<Entry<Long, E>> entryIterator() {
    return new Iterator<>() {
      private final long pages = tableLimit;
      private long pageNumber = 0;
      private long page = -1;
      // The next slot to examine in the current page, and the key found there, or -1.
      private long slot = 0;
      private long nextKey = -1;

      private boolean find() {
        if (nextKey >= 0) {
          return true;
        }
        while (true) {
          while (page < 0 && pageNumber < pages) {
            page = table.get(JAVA_LONG_UNALIGNED, pageNumber << 3) - 1;
            if (page < 0) {
              pageNumber++;
            }
          }
          if (page < 0) {
            return false;
          }
          while (slot <= slotMask) {
            long bits =
                presence.get(JAVA_LONG_UNALIGNED, presencePosition(page, slot)) >>> (slot & 63);
            if (bits != 0) {
              slot += Long.numberOfTrailingZeros(bits);
              nextKey = (pageNumber << pageShift) | slot;
              return true;
            }
            slot = (slot | 63) + 1;
          }
          pageNumber++;
          page = -1;
          slot = 0;
        }
      }

      @Override
      public boolean hasNext() {
        return find();
      }

      @Override
      public Entry<Long, E> next() {
        if (!find()) {
          throw new NoSuchElementException();
        }
        E value = values.read(dataType, valuePosition(page, slot));
        long key = nextKey;
        nextKey = -1;
        slot++;
        return Map.entry(key, value);
      }
    };
  }

  /** Persists the page count and size in the header of the page table. */
  public synchronized void flush() {
    MemorySegment header = table.header();
    header.set(JAVA_LONG_UNALIGNED, PAGE_COUNT_OFFSET, pageCount);
    header.set(JAVA_LONG_UNALIGNED, SIZE_OFFSET, size.get());
    header.set(JAVA_LONG_UNALIGNED, TABLE_LIMIT_OFFSET, tableLimit);
  }

  @Override
  public void close() {
    if (table.isClosed()) {
      return;
    }
    flush();
    table.close();
    presence.close();
    values.close();
  }
}
