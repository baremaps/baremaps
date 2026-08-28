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
import java.util.function.Consumer;

/**
 * A {@code Consumer} that accumulates the values it accepts.
 *
 * @param <T>
 */
public class AccumulatingConsumer<T> implements Consumer<T> {

  private final List<T> values = new ArrayList<>();

  /** Stores the accepted value. */
  @Override
  public void accept(T value) {
    values.add(value);
  }

  /**
   * Returns the accumulated values.
   *
   * @return the accumulated values.
   */
  public List<T> values() {
    return values;
  }
}
