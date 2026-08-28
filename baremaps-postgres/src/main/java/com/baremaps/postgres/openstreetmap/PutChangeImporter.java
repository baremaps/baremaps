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



import com.baremaps.openstreetmap.model.*;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A consumer for importing OpenStreetMap changes in a database. */
public class PutChangeImporter implements Consumer<Change> {

  private static final Logger logger = LoggerFactory.getLogger(PutChangeImporter.class);

  private final Repository<Long, Node> nodeRepository;
  private final Repository<Long, Way> wayRepository;
  private final Repository<Long, Relation> relationRepository;

  /**
   * Constructs a {@code SaveChangeConsumer}.
   *
   * @param nodeRepository the node table
   * @param wayRepository the way table
   * @param relationRepository the relation table
   */
  public PutChangeImporter(
      Repository<Long, Node> nodeRepository,
      Repository<Long, Way> wayRepository,
      Repository<Long, Relation> relationRepository) {
    this.nodeRepository = nodeRepository;
    this.wayRepository = wayRepository;
    this.relationRepository = relationRepository;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Change change) {
    var nodes = change.entities().stream()
        .filter(entity -> entity instanceof Node)
        .map(entity -> (Node) entity)
        .toList();
    var ways = change.entities().stream()
        .filter(entity -> entity instanceof Way)
        .map(entity -> (Way) entity)
        .toList();
    var relations = change.entities().stream()
        .filter(entity -> entity instanceof Relation)
        .map(entity -> (Relation) entity)
        .toList();
    try {
      switch (change.type()) {
        case CREATE, MODIFY -> {
          put(nodeRepository, nodes);
          put(wayRepository, ways);
          put(relationRepository, relations);
        }
        case DELETE -> {
          delete(nodeRepository, nodes);
          delete(wayRepository, ways);
          delete(relationRepository, relations);
        }
      }
    } catch (RepositoryException e) {
      logger.error("Error while saving changes", e);
    }
  }

  private <T extends Element> void put(Repository<Long, T> repository, List<T> entities)
      throws RepositoryException {
    if (!entities.isEmpty()) {
      repository.put(entities);
    }
  }

  private <T extends Element> void delete(Repository<Long, T> repository, List<T> entities)
      throws RepositoryException {
    List<Long> ids = entities.stream().map(Element::getId).toList();
    if (!ids.isEmpty()) {
      repository.delete(ids);
    }
  }

}
