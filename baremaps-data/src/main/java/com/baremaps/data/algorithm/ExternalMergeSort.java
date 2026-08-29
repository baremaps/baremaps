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

package com.baremaps.data.algorithm;

import com.baremaps.data.collection.DataList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Sorts a {@link DataList} too large for the heap: the input is sorted in batches that fit in
 * memory, each batch is written to a temporary list, and the batches are merged into the output.
 */
public class ExternalMergeSort {

  private ExternalMergeSort() {
    // Prevent instantiation
  }

  /**
   * Sorts the input into the output.
   *
   * @param tempLists supplies the temporary lists holding the sorted batches; they are cleared once
   *        merged
   * @param batchSize the number of elements sorted in memory at a time
   * @param distinct whether to drop the duplicates, as defined by the comparator
   * @param parallel whether to sort the batches with a parallel stream
   */
  public static <T> void sort(
      DataList<T> input,
      DataList<T> output,
      Comparator<T> comparator,
      Supplier<DataList<T>> tempLists,
      long batchSize,
      boolean distinct,
      boolean parallel) {
    List<DataList<T>> batches = new ArrayList<>();
    List<T> batch = new ArrayList<>();
    for (T element : input) {
      batch.add(element);
      if (batch.size() >= batchSize) {
        batches.add(sortBatch(batch, comparator, tempLists, distinct, parallel));
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      batches.add(sortBatch(batch, comparator, tempLists, distinct, parallel));
    }
    merge(batches, output, comparator, distinct);
  }

  private static <T> DataList<T> sortBatch(
      List<T> batch,
      Comparator<T> comparator,
      Supplier<DataList<T>> tempLists,
      boolean distinct,
      boolean parallel) {
    DataList<T> output = tempLists.get();
    Stream<T> stream = parallel ? batch.parallelStream() : batch.stream();
    stream = stream.sorted(comparator);
    if (distinct) {
      stream = stream.distinct();
    }
    stream.forEachOrdered(output::addIndexed);
    return output;
  }

  private static <T> void merge(
      List<DataList<T>> batches,
      DataList<T> output,
      Comparator<T> comparator,
      boolean distinct) {
    PriorityQueue<Cursor<T>> queue =
        new PriorityQueue<>(Math.max(1, batches.size()),
            (a, b) -> comparator.compare(a.head, b.head));
    for (DataList<T> batch : batches) {
      if (!batch.isEmpty()) {
        queue.add(new Cursor<>(batch));
      }
    }
    T last = null;
    while (!queue.isEmpty()) {
      Cursor<T> cursor = queue.poll();
      T value = cursor.head;
      if (!distinct || last == null || comparator.compare(value, last) != 0) {
        output.addIndexed(value);
        last = value;
      }
      if (cursor.advance()) {
        queue.add(cursor);
      }
    }
    for (DataList<T> batch : batches) {
      batch.clear();
    }
  }

  /** The next unread element of a batch. */
  private static final class Cursor<T> {

    private final DataList<T> list;

    private long index;

    private T head;

    Cursor(DataList<T> list) {
      this.list = list;
      this.index = 0;
      this.head = list.get(0);
    }

    /** Moves to the next element and returns whether there is one. */
    boolean advance() {
      index++;
      if (index >= list.size()) {
        head = null;
        return false;
      }
      head = list.get(index);
      return true;
    }
  }
}
