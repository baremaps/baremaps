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

import com.baremaps.openstreetmap.function.*;
import com.baremaps.openstreetmap.model.Block;
import com.baremaps.openstreetmap.stream.ConsumerUtils;
import com.baremaps.openstreetmap.stream.StreamUtils;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;

/** A utility class for reading an OpenStreetMap pbf file. */
public class PbfBlockReader implements PbfReader<Block> {

  private int buffer = Runtime.getRuntime().availableProcessors();

  private boolean geometry = false;

  private int srid = 4326;

  private Map<Long, Coordinate> coordinateMap;

  private Map<Long, List<Long>> referenceMap;

  @Override
  public int getBuffer() {
    return buffer;
  }

  @Override
  public PbfBlockReader setBuffer(int buffer) {
    this.buffer = buffer;
    return this;
  }

  @Override
  public boolean getGeometries() {
    return geometry;
  }

  @Override
  public PbfBlockReader setGeometries(boolean geometries) {
    this.geometry = geometries;
    return this;
  }

  @Override
  public int getSrid() {
    return srid;
  }

  @Override
  public PbfBlockReader setSrid(int srid) {
    this.srid = srid;
    return this;
  }

  @Override
  public Map<Long, Coordinate> getCoordinateMap() {
    return coordinateMap;
  }

  @Override
  public PbfBlockReader setCoordinateMap(Map<Long, Coordinate> coordinateMap) {
    this.coordinateMap = coordinateMap;
    return this;
  }

  @Override
  public Map<Long, List<Long>> getReferenceMap() {
    return referenceMap;
  }

  @Override
  public PbfBlockReader setReferenceMap(Map<Long, List<Long>> referenceMap) {
    this.referenceMap = referenceMap;
    return this;
  }

  @Override
  public Stream<Block> read(InputStream inputStream) {
    var blocks = StreamUtils.bufferInSourceOrder(
        StreamUtils.stream(new BlobIterator(inputStream)),
        new BlobToBlockMapper(),
        Runtime.getRuntime().availableProcessors());
    if (geometry) {
      // Initialize and chain the entity handlers
      var coordinateMapBuilder = new CoordinateMapBuilder(coordinateMap);
      var referenceMapBuilder = new ReferenceMapBuilder(referenceMap);
      var entityGeometryBuilder = new EntityGeometryBuilder(coordinateMap, referenceMap);
      var entityProjectionTransformer = new EntityProjectionTransformer(4326, srid);
      var entityHandler = coordinateMapBuilder
          .andThen(referenceMapBuilder)
          .andThen(entityGeometryBuilder)
          .andThen(entityProjectionTransformer);

      // Initialize the block mapper
      var blockMapper = ConsumerUtils.consumeThenReturn(new BlockEntitiesHandler(entityHandler));
      blocks = blocks.map(blockMapper);
    }
    return blocks;
  }
}
