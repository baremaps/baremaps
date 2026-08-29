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

/**
 * A consumer that applies another consumer to the entities of a change that have a given type.
 *
 * <p>
 * Applying a change to a database is done in passes, one entity type at a time, because a way can
 * only be written once the nodes it references are in place. Naming the type of a pass keeps the
 * passes from treading on each other's elements.
 *
 * @param <T> the type of the entities the consumer is applied to
 */
public class ChangeEntitiesHandler<T extends Entity> implements Consumer<Change> {

  private final Class<T> type;
  private final Consumer<? super T> consumer;

  /**
   * Constructs a consumer that applies the provided consumer to the entities of a change that have
   * the provided type.
   *
   * @param type the type of the entities to consume, {@code Entity.class} for all of them
   * @param consumer the consumer to apply
   */
  public ChangeEntitiesHandler(Class<T> type, Consumer<? super T> consumer) {
    this.type = type;
    this.consumer = consumer;
  }

  @Override
  public void accept(Change change) {
    for (Entity entity : change.entities()) {
      if (type.isInstance(entity)) {
        consumer.accept(type.cast(entity));
      }
    }
  }
}
