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



import com.baremaps.openstreetmap.model.Block;
import com.baremaps.openstreetmap.model.DataBlock;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.HeaderBlock;
import com.baremaps.openstreetmap.stream.StreamException;
import java.util.function.Consumer;

/** Represents an operation on the entities of blocks of different types. */
public class BlockEntitiesHandler implements Consumer<Block> {

  private final Consumer<Entity> consumer;

  /**
   * Constructs a block consumer that applies the specified consumer to the block entities.
   *
   * @param consumer the entity consumer
   */
  public BlockEntitiesHandler(Consumer<Entity> consumer) {
    this.consumer = consumer;
  }

  @Override
  public void accept(Block block) {
    if (block instanceof HeaderBlock headerBlock) {
      consumer.accept(headerBlock.getHeader());
      consumer.accept(headerBlock.getBound());
    } else if (block instanceof DataBlock dataBlock) {
      dataBlock.getDenseNodes().forEach(consumer);
      dataBlock.getNodes().forEach(consumer);
      dataBlock.getWays().forEach(consumer);
      dataBlock.getRelations().forEach(consumer);
    } else {
      throw new StreamException("Unknown block type.");
    }
  }

}
