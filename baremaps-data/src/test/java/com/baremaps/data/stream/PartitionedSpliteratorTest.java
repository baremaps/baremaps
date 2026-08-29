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

package com.baremaps.data.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PartitionedSpliteratorTest {

  @Test
  void testStreamRange() {
    PartitionedSpliterator<Integer> stream =
        new PartitionedSpliterator<>(IntStream.range(0, 1000).spliterator(), 250);
    for (int i = 0; i < 4; i++) {
      assertTrue(stream.tryAdvance(block -> {
      }));
    }
    assertFalse(stream.tryAdvance(block -> {
    }));

    stream = new PartitionedSpliterator<>(IntStream.range(0, 5000).spliterator(), 250);
    for (int i = 0; i < 20; i++) { // 5000/250
      assertTrue(stream.tryAdvance(block -> {
      }));
    }
    assertFalse(stream.tryAdvance(block -> {
    }));
  }

  @Test
  void testEstimateSize() {
    PartitionedSpliterator<Integer> stream =
        new PartitionedSpliterator<>(IntStream.range(0, 1000).spliterator(), 250);

    assertEquals(4, stream.estimateSize());

    stream = new PartitionedSpliterator<>(IntStream.range(0, 1000).spliterator(), 100);

    assertEquals(10, stream.estimateSize());
  }

  @Test
  void trySplitTest() {
    PartitionedSpliterator<Integer> stream =
        new PartitionedSpliterator<>(IntStream.range(0, 1000).spliterator(), 250);

    Spliterator<List<Integer>> a = stream.trySplit();
    Spliterator<List<Integer>> b = stream.trySplit();
    Spliterator<List<Integer>> c = stream.trySplit();
    Spliterator<List<Integer>> d = stream.trySplit();
    Spliterator<List<Integer>> e = stream.trySplit();
    AtomicInteger i = new AtomicInteger();
    a.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item); // cast necessary otherwise call is ambiguous
      i.getAndIncrement();
    }));
    b.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item);
      i.getAndIncrement();
    }));
    c.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item);
      i.getAndIncrement();
    }));
    d.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item);
      i.getAndIncrement();
    }));
    // Should have no remaining item
    e.forEachRemaining(s -> s.forEach(item -> fail()));
    stream.forEachRemaining(item -> fail());
    assertEquals(1000, i.get());
  }

  @Test
  void trySplitWithNonEquivalentElementInStreamTest() {
    PartitionedSpliterator<Integer> stream =
        new PartitionedSpliterator<>(IntStream.range(0, 600).spliterator(), 250);

    Spliterator<List<Integer>> a = stream.trySplit();
    Spliterator<List<Integer>> b = stream.trySplit();
    Spliterator<List<Integer>> c = stream.trySplit();
    Spliterator<List<Integer>> d = stream.trySplit();
    AtomicInteger i = new AtomicInteger();
    a.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item); // cast necessary otherwise call is ambiguous
      i.getAndIncrement();
    }));
    b.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item);
      i.getAndIncrement();
    }));
    c.forEachRemaining(s -> s.forEach(item -> {
      assertEquals(i.get(), (long) item);
      i.getAndIncrement();
    }));
    d.forEachRemaining(s -> s.forEach(item -> fail()));
    stream.forEachRemaining(item -> fail());
    assertEquals(600, i.get());
  }
}
