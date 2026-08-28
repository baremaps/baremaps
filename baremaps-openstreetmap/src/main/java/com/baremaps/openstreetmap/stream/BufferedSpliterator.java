/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.openstreetmap.stream;



import java.util.Spliterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A spliterator that buffers the completion of a spliterator of future elements and returns them
 * according to a user defined order.
 *
 * <p>
 * This code has been adapted from
 * {@link <a href="https://github.com/palantir/streams/">streams</a>} licensed under the Apache
 * License 2.0.
 *
 * <p>
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * @param <T> the type of elements returned by this {@code Spliterator}
 */
class BufferedSpliterator<T> implements Spliterator<CompletableFuture<T>> {

  private final CompletionOrder completionOrder;

  private final Spliterator<CompletableFuture<T>> spliterator;

  private final int bufferSize;

  private final BlockingQueue<CompletableFuture<T>> buffer;

  private int pending = 0;

  /**
   * Constructs a {@code BufferedSpliterator} from a spliterator of futures elements.
   *
   * @param spliterator the spliterator to buffer
   * @param bufferSize the buffer size
   * @param completionOrder the completion order
   */
  public BufferedSpliterator(Spliterator<CompletableFuture<T>> spliterator, int bufferSize,
      CompletionOrder completionOrder) {
    this.spliterator = spliterator;
    this.bufferSize = bufferSize;
    this.buffer = new ArrayBlockingQueue<>(bufferSize);
    this.completionOrder = completionOrder;
  }

  /** {@inheritDoc} */
  @Override
  public boolean tryAdvance(Consumer<? super CompletableFuture<T>> action) {
    fillBuffer();
    if (pending == 0) {
      return false;
    }
    try {
      CompletableFuture<T> future = buffer.take();
      pending--;
      action.accept(future);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StreamException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Spliterator<CompletableFuture<T>> trySplit() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public long estimateSize() {
    long estimate = pending + spliterator.estimateSize();
    if (estimate < 0) {
      return Long.MAX_VALUE;
    }
    return estimate;
  }

  /** {@inheritDoc} */
  @Override
  public int characteristics() {
    return spliterator.characteristics();
  }

  private void fillBuffer() {
    while (pending < bufferSize && spliterator
        .tryAdvance(future -> completionOrder.registerCompletion(future, buffer::add))) {
      pending++;
    }
  }

  /** Represents the completion order applied to a {@code BufferedSpliterator}. */
  public interface CompletionOrder {

    <T> void registerCompletion(CompletableFuture<T> future,
        Consumer<CompletableFuture<T>> resultConsumer);
  }

  /** An order that registers completions when futures are completed. */
  enum InCompletionOrder implements CompletionOrder {
    INSTANCE;

    @Override
    public <T> void registerCompletion(CompletableFuture<T> future,
        Consumer<CompletableFuture<T>> resultConsumer) {
      future.whenComplete((result, error) -> resultConsumer.accept(future));
    }
  }

  /** An order that registers completion according to the order of the source. */
  enum InSourceOrder implements CompletionOrder {
    INSTANCE;

    @Override
    public <T> void registerCompletion(CompletableFuture<T> future,
        Consumer<CompletableFuture<T>> resultConsumer) {
      resultConsumer.accept(future);
    }
  }
}
