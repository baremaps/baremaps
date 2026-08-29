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



import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * A spliterator that hands out fixed-size batches of another spliterator when it is split.
 *
 * <p>
 * The wrapped spliterator has an unknown size and therefore cannot be split by the JDK. Splitting
 * eagerly into batches is what lets such a stream be processed in parallel.
 *
 * @param <T> the type of the elements
 */
public class BatchedSpliterator<T> implements Spliterator<T> {

  private final Spliterator<T> spliterator;
  private final int batchSize;

  /**
   * Creates a spliterator that hands out batches of the underlying spliterator.
   *
   * @param spliterator the underlying spliterator
   * @param batchSize the number of elements per batch
   */
  public BatchedSpliterator(Spliterator<T> spliterator, int batchSize) {
    this.spliterator = spliterator;
    this.batchSize = batchSize;
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    return this.spliterator.tryAdvance(action);
  }

  /**
   * Returns a spliterator covering the next batch, or null once the source is exhausted.
   *
   * @return a spliterator covering the elements of a batch
   */
  @Override
  public Spliterator<T> trySplit() {
    List<T> batch = new ArrayList<>();
    while (batch.size() < batchSize && tryAdvance(batch::add)) {
      // Do nothing
    }
    if (!batch.isEmpty()) {
      return Spliterators.spliterator(batch, characteristics());
    } else {
      return null;
    }
  }

  /**
   * Returns the size of the underlying spliterator.
   *
   * @return the size of the underlying spliterator
   */
  @Override
  public long estimateSize() {
    return spliterator.estimateSize();
  }

  /**
   * Returns the characteristics of the underlying spliterator, plus the sizing guarantees that
   * splitting into fixed batches provides.
   *
   * @return a representation of characteristics
   */
  @Override
  public int characteristics() {
    return spliterator.characteristics() | SIZED | SUBSIZED;
  }
}
