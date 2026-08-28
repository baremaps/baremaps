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

package com.baremaps.openstreetmap.function;



import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.model.Entity;
import java.util.function.Consumer;

/** Represents an operation on the entities of a change. */
public class ChangeEntitiesHandler implements Consumer<Change> {

  private final Consumer<Entity> consumer;

  /**
   * Constructs a consumer that applies the specified consumer to all the entities of a {@code
   * Change}.
   *
   * @param consumer
   */
  public ChangeEntitiesHandler(Consumer<Entity> consumer) {
    this.consumer = consumer;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Change change) {
    change.entities().forEach(consumer);
  }
}
