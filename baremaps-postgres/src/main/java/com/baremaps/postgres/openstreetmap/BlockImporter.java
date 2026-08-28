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
import com.baremaps.openstreetmap.stream.StreamException;
import java.util.function.Consumer;

/** A consumer for importing OpenStreetMap blocks in a database. */
public class BlockImporter implements Consumer<Block> {

  private final Repository<Long, Header> headerRepository;
  private final Repository<Long, Node> nodeRepository;
  private final Repository<Long, Way> wayRepository;
  private final Repository<Long, Relation> relationRepository;

  /**
   * Constructs a {@code SaveBlockConsumer}.
   *
   * @param headerRepository the header table
   * @param nodeRepository the node table
   * @param wayRepository the way table
   * @param relationRepository the relation table
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

  @Override
  public void accept(Block block) {
    try {
      if (block instanceof HeaderBlock headerBlock) {
        headerRepository.put(headerBlock.getHeader());
      } else if (block instanceof DataBlock dataBlock) {
        nodeRepository.copy(dataBlock.getDenseNodes());
        nodeRepository.copy(dataBlock.getNodes());
        wayRepository.copy(dataBlock.getWays());
        relationRepository.copy(dataBlock.getRelations());
      }
    } catch (Exception e) {
      throw new StreamException(e);
    }
  }
}
