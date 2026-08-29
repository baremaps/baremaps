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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.data.memory.Memory;
import com.baremaps.data.type.LongDataType;
import com.baremaps.data.type.StringDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Concurrent appends: every returned position or index reads back the value that produced it. */
class ConcurrencyTest {

  private static final int THREADS = 8;

  private static final int PER_THREAD = 50_000;

  private static final int SEGMENT = 1 << 12;

  private static <T> List<T> runAll(List<Callable<T>> tasks) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    try {
      var results = new ArrayList<T>();
      for (Future<T> future : executor.invokeAll(tasks)) {
        results.add(future.get());
      }
      return results;
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void appendOnlyLog() throws Exception {
    var log = new AppendOnlyLog<>(new StringDataType(), Memory.offHeap(SEGMENT));
    var tasks = new ArrayList<Callable<long[]>>();
    for (int t = 0; t < THREADS; t++) {
      int thread = t;
      tasks.add(() -> {
        var positions = new long[PER_THREAD];
        for (int i = 0; i < PER_THREAD; i++) {
          positions[i] = log.addPositioned(thread + "-" + i + "-" + "x".repeat(i % 40));
        }
        return positions;
      });
    }
    var results = runAll(tasks);
    assertEquals((long) THREADS * PER_THREAD, log.size());
    for (int t = 0; t < THREADS; t++) {
      for (int i = 0; i < PER_THREAD; i++) {
        assertEquals(t + "-" + i + "-" + "x".repeat(i % 40), log.getPositioned(results.get(t)[i]));
      }
    }
    assertEquals(log.size(), log.stream().count());
  }

  @Test
  void fixedSizeDataList() throws Exception {
    var list = new FixedSizeDataList<>(new LongDataType(), Memory.offHeap(SEGMENT));
    var tasks = new ArrayList<Callable<long[]>>();
    for (int t = 0; t < THREADS; t++) {
      long thread = t;
      tasks.add(() -> {
        var indexes = new long[PER_THREAD];
        for (int i = 0; i < PER_THREAD; i++) {
          indexes[i] = list.addIndexed(thread * PER_THREAD + i);
        }
        return indexes;
      });
    }
    var results = runAll(tasks);
    assertEquals((long) THREADS * PER_THREAD, list.size());
    for (int t = 0; t < THREADS; t++) {
      for (int i = 0; i < PER_THREAD; i++) {
        assertEquals((long) t * PER_THREAD + i, list.get(results.get(t)[i]));
      }
    }
  }

  @Test
  void concurrentReadersWhileAppending() throws Exception {
    var list = new FixedSizeDataList<>(new LongDataType(), Memory.offHeap(SEGMENT));
    var tasks = new ArrayList<Callable<Boolean>>();
    tasks.add(() -> {
      for (long i = 0; i < THREADS * PER_THREAD; i++) {
        list.add(i);
      }
      return true;
    });
    for (int t = 1; t < THREADS; t++) {
      tasks.add(() -> {
        // Readers only look below the published size and must always see consistent values.
        for (int round = 0; round < 1000; round++) {
          long size = list.size();
          for (long i = 0; i < size; i += 997) {
            if (list.get(i) != i) {
              return false;
            }
          }
        }
        return true;
      });
    }
    for (boolean ok : runAll(tasks)) {
      assertEquals(true, ok);
    }
  }

  @Test
  void denseDataMap() throws Exception {
    var map = new DenseDataMap<>(new LongDataType(), 6, Memory.offHeap(SEGMENT),
        Memory.offHeap(SEGMENT), Memory.offHeap(SEGMENT));
    // Interleaved keys: neighbouring threads write the same bitmap words and allocate the same
    // pages at the same time.
    var tasks = new ArrayList<Callable<Boolean>>();
    for (int t = 0; t < THREADS; t++) {
      int thread = t;
      tasks.add(() -> {
        for (int i = 0; i < PER_THREAD; i++) {
          long key = (long) i * THREADS + thread;
          map.put(key, key);
        }
        for (int i = 0; i < PER_THREAD; i++) {
          long key = (long) i * THREADS + thread;
          if (map.get(key) != key) {
            return false;
          }
        }
        return true;
      });
    }
    for (boolean ok : runAll(tasks)) {
      assertEquals(true, ok);
    }
    assertEquals((long) THREADS * PER_THREAD, map.size());
    map.close();
  }
}
