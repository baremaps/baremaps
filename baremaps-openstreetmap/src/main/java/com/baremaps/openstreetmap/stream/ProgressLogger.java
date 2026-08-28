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



import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for peeking progress when processing a {@code Stream}.
 *
 * @param <T>
 */
public class ProgressLogger<T> implements Consumer<T> {

  private static final Logger logger = LoggerFactory.getLogger(ProgressLogger.class);

  private final AtomicLong position = new AtomicLong(0);

  private final long size;

  private final int tick;

  // A volatile does not guarantee atomicity but blocking with an AtomicLong is not worth it.
  private volatile long timestamp;

  /**
   * Constructs a {@code StreamProgress} that periodically logs progress.
   *
   * @param size the size of the stream
   * @param tick the tick in milliseconds at with progress is logged
   */
  public ProgressLogger(Long size, Integer tick) {
    this.size = size;
    this.tick = tick;
    this.timestamp = System.currentTimeMillis();
  }

  /**
   * Accepts stream element and increments progress.
   *
   * @param e the element
   */
  @Override
  public void accept(T e) {
    long progress = position.incrementAndGet();
    long t = System.currentTimeMillis();
    long l = timestamp;
    if (size >= 0 && t - l >= tick) {
      timestamp = t;
      double p = Math.round(progress * 10000d / size) / 100d;
      logger.info("{}%", p);
    }
    if (size >= 0 && progress == size) {
      double p = 100d;
      logger.info("{}%", p);
    }
  }
}
