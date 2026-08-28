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

package com.baremaps.openstreetmap.stream;



import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A spliterator that partition another spliterator.
 *
 * @param <T> the type of elements returned by this {@code Spliterator}
 */
public class PartitionedSpliterator<T> implements Spliterator<List<T>> {

  private final Spliterator<T> spliterator;

  private final int partitionSize;

  /**
   * Constructs a {@code PartitionedSpliterator} from another spliterator
   *
   * @param spliterator the spliterator to partition
   * @param partitionSize the partition size
   */
  public PartitionedSpliterator(Spliterator<T> spliterator, int partitionSize) {
    this.spliterator = spliterator;
    this.partitionSize = partitionSize;
  }

  /** {@inheritDoc} */
  @Override
  public boolean tryAdvance(Consumer<? super List<T>> action) {
    var list = new ArrayList<T>(partitionSize);
    int size = 0;
    while (size < partitionSize && spliterator.tryAdvance(list::add)) {
      size++;
    }
    if (size == 0) {
      return false;
    }
    action.accept(list);
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public Spliterator<List<T>> trySplit() {
    HoldingConsumer<List<T>> consumer = new HoldingConsumer<>();
    tryAdvance(consumer);
    return Stream.ofNullable(consumer.value()).spliterator();
  }

  /** {@inheritDoc} */
  @Override
  public long estimateSize() {
    return spliterator.estimateSize() / partitionSize;
  }

  /** {@inheritDoc} */
  @Override
  public int characteristics() {
    return spliterator.characteristics();
  }
}
