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



import java.util.function.Consumer;

/**
 * A {@code Consumer} that holds the latest value it accepted, so a caller can read back what a
 * spliterator handed over.
 *
 * @param <T> the type of the value
 */
public class HoldingConsumer<T> implements Consumer<T> {

  private T value;

  @Override
  public void accept(T value) {
    this.value = value;
  }

  /**
   * Returns the value of the last accepted element, or null if nothing was accepted.
   *
   * @return the held value
   */
  public T value() {
    return value;
  }
}
