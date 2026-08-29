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
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A spliterator that groups the elements of another spliterator into fixed-size lists.
 *
 * @param <T> the type of the elements being grouped
 */
public class PartitionedSpliterator<T> implements Spliterator<List<T>> {

  private final Spliterator<T> spliterator;

  private final int partitionSize;

  /**
   * Constructs a spliterator that groups the elements of another one.
   *
   * @param spliterator the spliterator to partition
   * @param partitionSize the number of elements per partition
   */
  public PartitionedSpliterator(Spliterator<T> spliterator, int partitionSize) {
    this.spliterator = spliterator;
    this.partitionSize = partitionSize;
  }

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

  @Override
  public Spliterator<List<T>> trySplit() {
    HoldingConsumer<List<T>> consumer = new HoldingConsumer<>();
    tryAdvance(consumer);
    return Stream.ofNullable(consumer.value()).spliterator();
  }

  @Override
  public long estimateSize() {
    return spliterator.estimateSize() / partitionSize;
  }

  @Override
  public int characteristics() {
    return spliterator.characteristics();
  }
}
