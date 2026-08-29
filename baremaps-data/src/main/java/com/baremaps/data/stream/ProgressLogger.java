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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A consumer that logs how far a stream of known size has progressed, at most once per tick.
 *
 * @param <T> the type of the elements
 */
public class ProgressLogger<T> implements Consumer<T> {

  private static final Logger logger = LoggerFactory.getLogger(ProgressLogger.class);

  private final AtomicLong position = new AtomicLong(0);

  private final long size;

  private final long tick;

  // Reading and writing this timestamp is not atomic, so two threads can log the same tick. That
  // is preferable to making every element of a parallel stream contend on a lock.
  private volatile long timestamp;

  /**
   * Constructs a consumer that logs progress at most once per tick.
   *
   * @param size the number of elements in the stream
   * @param tick the minimum delay between two logs, in milliseconds
   */
  public ProgressLogger(long size, long tick) {
    this.size = size;
    this.tick = tick;
    this.timestamp = System.currentTimeMillis();
  }

  /** Counts the element and logs the progress if a tick has elapsed or the stream is complete. */
  @Override
  public void accept(T element) {
    if (size < 0) {
      return;
    }
    long progress = position.incrementAndGet();
    long now = System.currentTimeMillis();
    if (progress == size) {
      logger.info("100%");
    } else if (now - timestamp >= tick) {
      timestamp = now;
      logger.info("{}%", Math.round(progress * 10000d / size) / 100d);
    }
  }
}
