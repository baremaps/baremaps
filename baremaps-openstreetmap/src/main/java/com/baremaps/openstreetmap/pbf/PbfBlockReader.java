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

package com.baremaps.openstreetmap.pbf;

import static com.baremaps.data.stream.ConsumerUtils.consumeThenReturn;

import com.baremaps.data.stream.StreamException;
import com.baremaps.data.stream.StreamUtils;
import com.baremaps.openstreetmap.EntityReader;
import com.baremaps.openstreetmap.GeometryOptions;
import com.baremaps.openstreetmap.model.Block;
import com.baremaps.openstreetmap.model.Entity;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/** Reads an OpenStreetMap PBF file as a stream of blocks. */
public class PbfBlockReader implements EntityReader<Block> {

  private final UnaryOperator<Block> blockHandler;

  /** Creates a reader that leaves geometries unset. */
  public PbfBlockReader() {
    this.blockHandler = UnaryOperator.identity();
  }

  /**
   * Creates a reader that sets the geometry of the elements it reads.
   *
   * @param geometryOptions the geometry options
   */
  public PbfBlockReader(GeometryOptions geometryOptions) {
    Consumer<Entity> entityHandler = geometryOptions.entityHandler();
    this.blockHandler = consumeThenReturn(block -> block.entities().forEach(entityHandler));
  }

  @Override
  public Stream<Block> read(InputStream input) {
    // Blobs are decoded in parallel, but delivered in file order: geometry building relies on
    // nodes being seen before the ways that reference them (see GeometryOptions#entityHandler).
    return StreamUtils.bufferInSourceOrder(
        StreamUtils.stream(new BlobIterator(input)),
        PbfBlockReader::toBlock,
        Runtime.getRuntime().availableProcessors())
        .map(blockHandler);
  }

  private static Block toBlock(Blob blob) {
    try {
      return switch (blob.type()) {
        case "OSMHeader" -> HeaderBlockReader.read(blob);
        case "OSMData" -> DataBlockReader.read(blob);
        default -> throw new StreamException("Unknown blob type: " + blob.type());
      };
    } catch (StreamException e) {
      throw e;
    } catch (Exception e) {
      throw new StreamException(e);
    }
  }
}
