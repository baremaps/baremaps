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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchedSpliteratorTest {

  final int spliteratorSize = 105;
  final int batchSize = 10;

  BatchedSpliterator<Integer> spliterator;

  @BeforeEach
  void setUp() {
    List<Integer> ints = new ArrayList<>();
    for (int i = 0; i < spliteratorSize; i++) {
      ints.add(i);
    }
    spliterator =
        new BatchedSpliterator<>(IntStream.range(0, spliteratorSize).spliterator(), batchSize) {
          int i = 0;

          @Override
          public boolean tryAdvance(Consumer<? super Integer> consumer) {
            if (i++ < spliteratorSize) {
              consumer.accept(i);
              return true;
            }
            return false;
          }
        };
  }

  @Test
  void tryAdvance() {
    for (int i = 0; i < spliteratorSize; i++) {
      assertTrue(spliterator.tryAdvance(block -> {
      }));
    }
    assertFalse(spliterator.tryAdvance(block -> {
    }));
  }

  @Test
  void forEachRemaining() {
    var values = new ArrayList<Integer>();
    spliterator.forEachRemaining(values::add);
    assertEquals(values.size(), spliteratorSize);
  }

  @Test
  void trySplit() {
    Spliterator<Integer> s;
    for (int i = 0; i < spliteratorSize / batchSize; i++) {
      s = spliterator.trySplit();
      assertNotNull(s);
      assertEquals(s.estimateSize(), batchSize);
    }
    assertNotNull(spliterator);
    assertEquals(spliterator.trySplit().estimateSize(), spliteratorSize % batchSize);
    assertNull(spliterator.trySplit());
  }

  @Test
  void estimateSize() {
    assertEquals(spliterator.estimateSize(), spliteratorSize);
  }
}
