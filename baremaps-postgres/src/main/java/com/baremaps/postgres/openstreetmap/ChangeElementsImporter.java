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

import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.model.Element;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A consumer for importing change nodes in a database. */
public class ChangeElementsImporter<T extends Element> implements Consumer<Change> {

  private static final Logger logger = LoggerFactory.getLogger(ChangeElementsImporter.class);

  private final Class<T> type;

  private final Repository<Long, T> nodeRepository;

  /**
   * Constructs a {@code ChangeNodesImporter}.
   *
   * @param nodeRepository the node repository
   */
  public ChangeElementsImporter(
      Class<T> type,
      Repository<Long, T> nodeRepository) {
    this.type = type;
    this.nodeRepository = nodeRepository;
  }

  /** {@inheritDoc} */
  @Override
  public void accept(Change change) {
    switch (change.type()) {
      case CREATE, MODIFY -> put(change);
      case DELETE -> delete(change);
    }
  }

  private void put(Change change) {
    var nodes = change.entities().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
    if (!nodes.isEmpty()) {
      try {
        nodeRepository.put(nodes);
      } catch (RepositoryException e) {
        logger.error("Failed to save nodes", e);
      }
    }
  }

  private void delete(Change change) {
    var nodes = change.entities().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .map(Element::getId)
        .toList();
    if (!nodes.isEmpty()) {
      try {
        nodeRepository.delete(nodes);
      } catch (RepositoryException e) {
        logger.error("Failed to delete nodes", e);
      }
    }
  }
}
