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

package com.baremaps.postgres.openstreetmap;

import com.baremaps.data.stream.StreamException;
import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.model.Element;
import java.util.List;
import java.util.function.Consumer;

/**
 * Applies the entities of one type carried by an OpenStreetMap change to a repository.
 *
 * <p>
 * An importer handles a single entity type so that a caller can interleave the passes it needs
 * between them: a way can only be given a geometry once the nodes of the change are in place. Chain
 * three importers with {@link Consumer#andThen} to apply a change whole.
 *
 * @param <T> the type of entity this importer applies
 */
public class ChangeImporter<T extends Element> implements Consumer<Change> {

  /**
   * How the entities created or modified by a change reach the table.
   *
   * <p>
   * Both modes end with the table holding the entities of the change; they differ in what they
   * cost.
   */
  public enum Mode {

    /** Upserts the entities one statement at a time. Suited to the small changes of an update. */
    PUT {
      @Override
      <T extends Element> void apply(Repository<Long, T> repository, List<T> entities)
          throws RepositoryException {
        repository.put(entities);
      }
    },

    /**
     * Deletes the entities and copies them back in binary form. The copy interface does not replace
     * the rows it collides with, hence the delete; it is worth the extra statement for the large
     * changesets an import replays, and wasteful for the handful of entities an update carries.
     */
    COPY {
      @Override
      <T extends Element> void apply(Repository<Long, T> repository, List<T> entities)
          throws RepositoryException {
        repository.delete(ids(entities));
        repository.copy(entities);
      }
    };

    abstract <T extends Element> void apply(Repository<Long, T> repository, List<T> entities)
        throws RepositoryException;
  }

  private final Class<T> type;
  private final Repository<Long, T> repository;
  private final Mode mode;

  /**
   * Constructs a {@code ChangeImporter}.
   *
   * @param type the entity type to apply, the others being left to the other importers of the chain
   * @param repository the repository holding that type
   * @param mode how created and modified entities reach the table
   */
  public ChangeImporter(Class<T> type, Repository<Long, T> repository, Mode mode) {
    this.type = type;
    this.repository = repository;
    this.mode = mode;
  }

  /**
   * {@inheritDoc}
   *
   * @throws StreamException if the change cannot be applied. A change that is dropped rather than
   *         applied leaves the database behind the replication sequence number that is about to be
   *         recorded as reached, and nothing afterwards would notice.
   */
  @Override
  public void accept(Change change) {
    var entities = change.entities().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
    if (entities.isEmpty()) {
      return;
    }
    try {
      switch (change.type()) {
        case CREATE, MODIFY -> mode.apply(repository, entities);
        case DELETE -> repository.delete(ids(entities));
      }
    } catch (RepositoryException e) {
      throw new StreamException(e);
    }
  }

  private static <T extends Element> List<Long> ids(List<T> entities) {
    return entities.stream().map(Element::getId).toList();
  }
}
