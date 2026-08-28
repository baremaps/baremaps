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

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Utility methods for dealing with consumers. */
public class ConsumerUtils {

  private ConsumerUtils() {
    // Prevent instantiation
  }

  /**
   * Turns a consumer into an identity function with a side effect, so it can be used in a stream
   * {@code map} where {@code peek} would be discouraged.
   *
   * @param consumer the consumer
   * @param <T> the type
   * @return the function
   */
  public static <T> UnaryOperator<T> consumeThenReturn(Consumer<T> consumer) {
    return t -> {
      consumer.accept(t);
      return t;
    };
  }
}
