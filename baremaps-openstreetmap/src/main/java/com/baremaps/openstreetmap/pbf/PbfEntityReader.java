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



import com.baremaps.openstreetmap.model.DataBlock;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.HeaderBlock;
import com.baremaps.openstreetmap.stream.StreamException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;

/** A utility class for flattening the blocks streamed by a {@link PbfBlockReader}. */
public class PbfEntityReader implements PbfReader<Entity> {

  private final PbfBlockReader reader;

  /**
   * Constructs an entity reader from a block reader.
   */
  public PbfEntityReader() {
    this.reader = new PbfBlockReader();
  }

  @Override
  public int getBuffer() {
    return reader.getBuffer();
  }

  @Override
  public PbfEntityReader setBuffer(int buffer) {
    reader.setBuffer(buffer);
    return this;
  }

  @Override
  public boolean getGeometries() {
    return reader.getGeometries();
  }

  @Override
  public PbfEntityReader setGeometries(boolean geometries) {
    reader.setGeometries(geometries);
    return this;
  }

  @Override
  public int getSrid() {
    return reader.getSrid();
  }

  @Override
  public PbfEntityReader setSrid(int srid) {
    reader.setSrid(srid);
    return this;
  }

  @Override
  public Map<Long, Coordinate> getCoordinateMap() {
    return reader.getCoordinateMap();
  }

  @Override
  public PbfEntityReader setCoordinateMap(Map<Long, Coordinate> coordinateMap) {
    reader.setCoordinateMap(coordinateMap);
    return this;
  }

  @Override
  public Map<Long, List<Long>> getReferenceMap() {
    return reader.getReferenceMap();
  }

  @Override
  public PbfEntityReader setReferenceMap(Map<Long, List<Long>> referenceMap) {
    reader.setReferenceMap(referenceMap);
    return this;
  }

  /**
   * Creates an ordered stream of entities.
   *
   * @param inputStream an osm pbf {@link InputStream}
   * @return a stream of blocks
   */
  @Override
  public Stream<Entity> read(InputStream inputStream) {
    return reader.read(inputStream).flatMap(block -> {
      try {
        Stream.Builder<Entity> entities = Stream.builder();
        if (block instanceof HeaderBlock headerBlock) {
          entities.add(headerBlock.getHeader());
          entities.add(headerBlock.getBound());
        } else if (block instanceof DataBlock dataBlock) {
          dataBlock.getDenseNodes().forEach(entities::add);
          dataBlock.getNodes().forEach(entities::add);
          dataBlock.getWays().forEach(entities::add);
          dataBlock.getRelations().forEach(entities::add);
        } else {
          throw new StreamException("Unknown block type.");
        }
        return entities.build();
      } catch (Exception e) {
        throw new StreamException(e);
      }
    });
  }
}
