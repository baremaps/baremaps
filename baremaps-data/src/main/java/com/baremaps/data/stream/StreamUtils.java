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

import com.baremaps.data.stream.BufferedSpliterator.CompletionOrder;
import com.baremaps.data.stream.BufferedSpliterator.InCompletionOrder;
import com.baremaps.data.stream.BufferedSpliterator.InSourceOrder;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for parallelizing streams whose size is unknown.
 *
 * <p>
 * The streams baremaps consumes are read from files that do not announce how many elements they
 * hold. The JDK cannot split such a stream, so {@code stream.parallel()} runs single-threaded on
 * it. The methods here work around that by splitting eagerly ({@link #parallel}) or by mapping
 * elements asynchronously into a bounded buffer ({@code bufferIn...Order}).
 */
public class StreamUtils {

  private StreamUtils() {
    // Prevent instantiation
  }

  /**
   * Creates an ordered sequential stream from an iterator of unknown size.
   *
   * @param iterator the iterator
   * @param <T> the type of the elements
   * @return an ordered sequential stream
   */
  public static <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
        false);
  }

  /**
   * Parallelizes a stream of unknown size, handing one element at a time to the workers.
   *
   * @param stream the stream to parallelize
   * @param <T> the type of the elements
   * @return a parallel stream
   */
  public static <T> Stream<T> parallel(Stream<T> stream) {
    return parallel(stream, 1);
  }

  /**
   * Parallelizes a stream of unknown size, handing batches of the given size to the workers. Larger
   * batches amortize the hand-off when elements are cheap to process.
   *
   * @param stream the stream to parallelize
   * @param batchSize the number of elements handed over at once
   * @param <T> the type of the elements
   * @return a parallel stream
   */
  public static <T> Stream<T> parallel(Stream<T> stream, int batchSize) {
    return StreamSupport.stream(new BatchedSpliterator<>(stream.spliterator(), batchSize), true);
  }

  /**
   * Maps the elements of a stream asynchronously, returning them in the order they complete. Use
   * this when the consumer does not care about order and slow elements should not hold up fast
   * ones.
   *
   * @param stream the stream to map
   * @param asyncMapper the mapping applied on a worker thread
   * @param bufferSize the number of mappings allowed to be in flight
   * @param <T> the type of the source elements
   * @param <U> the type of the mapped elements
   * @return a stream of the mapped elements, in completion order
   */
  public static <T, U> Stream<U> bufferInCompletionOrder(Stream<T> stream,
      Function<T, U> asyncMapper, int bufferSize) {
    return buffer(stream, asyncMapper, InCompletionOrder.INSTANCE, bufferSize);
  }

  /**
   * Maps the elements of a stream asynchronously, returning them in the order of the source. Use
   * this when the consumer depends on the order of the file being read.
   *
   * @param stream the stream to map
   * @param asyncMapper the mapping applied on a worker thread
   * @param bufferSize the number of mappings allowed to be in flight
   * @param <T> the type of the source elements
   * @param <U> the type of the mapped elements
   * @return a stream of the mapped elements, in source order
   */
  public static <T, U> Stream<U> bufferInSourceOrder(Stream<T> stream, Function<T, U> asyncMapper,
      int bufferSize) {
    return buffer(stream, asyncMapper, InSourceOrder.INSTANCE, bufferSize);
  }

  private static <T, U> Stream<U> buffer(Stream<T> stream, Function<T, U> asyncMapper,
      CompletionOrder completionOrder, int bufferSize) {
    Stream<CompletableFuture<U>> asyncStream =
        stream.map(t -> CompletableFuture.supplyAsync(() -> asyncMapper.apply(t)));
    Stream<CompletableFuture<U>> buffered = StreamSupport.stream(
        new BufferedSpliterator<>(asyncStream.spliterator(), bufferSize, completionOrder),
        asyncStream.isParallel());
    return buffered.map(StreamUtils::join);
  }

  /** Unwraps a completed future, rethrowing its failure as an unchecked {@link StreamException}. */
  private static <T> T join(CompletableFuture<T> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw new StreamException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StreamException(e);
    }
  }

  /**
   * Groups the elements of a stream into lists of the given size. The last list is shorter when the
   * stream does not divide evenly.
   *
   * @param stream the stream to partition
   * @param partitionSize the number of elements per partition
   * @param <T> the type of the elements
   * @return a stream of partitions
   */
  public static <T> Stream<List<T>> partition(Stream<T> stream, int partitionSize) {
    return StreamSupport.stream(
        new PartitionedSpliterator<>(stream.spliterator(), partitionSize),
        stream.isParallel());
  }
}
