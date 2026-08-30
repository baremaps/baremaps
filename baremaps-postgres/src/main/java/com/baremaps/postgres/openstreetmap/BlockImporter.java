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
import com.baremaps.openstreetmap.model.Block;
import com.baremaps.openstreetmap.model.DataBlock;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.HeaderBlock;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import java.util.function.Consumer;

/**
 * Stores the blocks of an OpenStreetMap PBF file in a database.
 *
 * <p>
 * A data block holds entities that have never been seen before, so its rows go in through the
 * binary copy interface rather than through inserts that would check for a conflict on every one of
 * them.
 */
public class BlockImporter implements Consumer<Block> {

  private final Repository<Long, Header> headerRepository;
  private final Repository<Long, Node> nodeRepository;
  private final Repository<Long, Way> wayRepository;
  private final Repository<Long, Relation> relationRepository;

  /**
   * Constructs a {@code BlockImporter}.
   *
   * @param headerRepository the header repository
   * @param nodeRepository the node repository
   * @param wayRepository the way repository
   * @param relationRepository the relation repository
   */
  public BlockImporter(
      Repository<Long, Header> headerRepository,
      Repository<Long, Node> nodeRepository,
      Repository<Long, Way> wayRepository,
      Repository<Long, Relation> relationRepository) {
    this.headerRepository = headerRepository;
    this.nodeRepository = nodeRepository;
    this.wayRepository = wayRepository;
    this.relationRepository = relationRepository;
  }

  /**
   * {@inheritDoc}
   *
   * @throws StreamException if the block cannot be stored
   */
  @Override
  public void accept(Block block) {
    try {
      switch (block) {
        case HeaderBlock headerBlock -> headerRepository.put(headerBlock.header());
        case DataBlock dataBlock -> {
          nodeRepository.copy(dataBlock.nodes());
          wayRepository.copy(dataBlock.ways());
          relationRepository.copy(dataBlock.relations());
        }
      }
    } catch (RepositoryException e) {
      throw new StreamException(e);
    }
  }
}
