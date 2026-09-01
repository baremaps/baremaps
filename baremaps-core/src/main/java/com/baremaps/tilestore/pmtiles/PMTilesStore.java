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

package com.baremaps.tilestore.pmtiles;

import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.pmtiles.Compression;
import com.baremaps.pmtiles.PMTilesReader;
import com.baremaps.pmtiles.PMTilesWriter;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * A {@code TileStore} over a PMTiles archive.
 *
 * <p>
 * An archive is opened either to be read or to be written, never both, because PMTiles is written
 * in one pass: the directories that address the tiles are laid out once every tile is known. Which
 * of the two an archive was opened for is what the constructor says.
 */
public class PMTilesStore implements TileStore<ByteBuffer> {

  private final PMTilesReader reader;

  private final PMTilesWriter writer;

  /**
   * Opens an existing archive for reading.
   *
   * @param path the path of the archive
   * @throws TileStoreException if the archive cannot be opened
   */
  public PMTilesStore(Path path) throws TileStoreException {
    try {
      this.reader = new PMTilesReader(path);
      this.writer = null;
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Opens an archive for writing, describing it with a tileset.
   *
   * @param path the path of the archive
   * @param tileset what the archive holds
   * @throws TileStoreException if the archive cannot be opened
   */
  public PMTilesStore(Path path, Tileset tileset) throws TileStoreException {
    this.reader = null;
    try {
      var metadata = new HashMap<String, Object>();
      metadata.put("name", tileset.getName());
      metadata.put("type", "baselayer");
      metadata.put("version", tileset.getVersion());
      metadata.put("description", tileset.getDescription());
      metadata.put("attribution", tileset.getAttribution());
      metadata.put("vector_layers", tileset.getVectorLayers());

      var minZoom = Optional.ofNullable(tileset.getMinzoom()).orElse(0);
      var maxZoom = Optional.ofNullable(tileset.getMaxzoom()).orElse(14);
      var bounds = Optional.ofNullable(tileset.getBounds()).orElse(List.of(-180d, -90d, 180d, 90d));
      var center = Optional.ofNullable(tileset.getCenter()).orElse(List.of(0d, 0d, 3d));

      writer = new PMTilesWriter(path);
      writer.setMetadata(metadata);
      writer.setZoomRange(minZoom, maxZoom);
      writer.setBounds(bounds.get(0), bounds.get(1), bounds.get(2), bounds.get(3));
      writer.setCenter(center.get(2).intValue(), center.get(0), center.get(1));
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  /**
   * Reads a tile, or returns {@code null} when the archive does not hold it.
   *
   * <p>
   * The bytes come back as the tile itself rather than as the archive stores it: the compression a
   * PMTiles archive applies to its tiles is a property of the container, and a caller reading an
   * archive of images should not have to know that the container gzipped them.
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    if (reader == null) {
      throw new UnsupportedOperationException("This archive was opened for writing");
    }
    try {
      var blob = reader.getTile(tileCoord.z(), tileCoord.x(), tileCoord.y());
      if (blob == null) {
        return null;
      }
      var compression = reader.getHeader().tileCompression();
      if (compression == Compression.NONE) {
        return blob;
      }
      try (var input = compression.decompress(new ByteArrayInputStream(blob.array()))) {
        return ByteBuffer.wrap(input.readAllBytes());
      }
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  @Override
  public void write(TileCoord tileCoord, ByteBuffer blob) throws TileStoreException {
    if (writer == null) {
      throw new UnsupportedOperationException("This archive was opened for reading");
    }
    try {
      writer.setTile(tileCoord.z(), tileCoord.x(), tileCoord.y(), blob.array());
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }

  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws TileStoreException {
    try {
      if (reader != null) {
        reader.close();
        return;
      }
      writer.write();
    } catch (IOException e) {
      throw new TileStoreException(e);
    }
  }
}
