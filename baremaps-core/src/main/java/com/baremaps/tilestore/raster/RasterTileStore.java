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

package com.baremaps.tilestore.raster;

import com.baremaps.maplibre.vectortile.Feature;
import com.baremaps.maplibre.vectortile.Layer;
import com.baremaps.maplibre.vectortile.Tile;
import com.baremaps.maplibre.vectortile.VectorTileEncoder;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * A read-only {@code TileStore} derived from the elevation data of a GeoTIFF file.
 *
 * <p>
 * The elevation is a source, not a store: tiles are computed on demand and there is nothing to
 * write to or delete from.
 *
 * @param <T> the type of the tiles
 */
abstract class RasterTileStore<T> implements TileStore<T> {

  /** The side of a tile in pixels. */
  protected static final int TILE_SIZE = 256;

  protected final GeoTiffReader geoTiffReader;

  protected RasterTileStore(GeoTiffReader geoTiffReader) {
    this.geoTiffReader = geoTiffReader;
  }

  /** Encodes features as a single-layer vector tile, gzipped as the tile protocol expects. */
  protected static ByteBuffer encodeLayer(String name, List<Feature> features) throws IOException {
    var vectorTile = new VectorTileEncoder().encodeTile(new Tile(List.of(new Layer(name, 4096,
        features))));
    try (var bytes = new ByteArrayOutputStream()) {
      try (var gzip = new GZIPOutputStream(bytes)) {
        vectorTile.writeTo(gzip);
      }
      return ByteBuffer.wrap(bytes.toByteArray());
    }
  }

  /** This store is read-only: the tiles are derived from the elevation data. */
  @Override
  public void write(TileCoord tileCoord, T blob) throws TileStoreException {
    throw new UnsupportedOperationException("The raster tile store is read only");
  }

  /** This store is read-only: the tiles are derived from the elevation data. */
  @Override
  public void delete(TileCoord tileCoord) throws TileStoreException {
    throw new UnsupportedOperationException("The raster tile store is read only");
  }

  /**
   * Does nothing: the reader is owned by the caller that opened it, which commonly shares one
   * reader between several of these stores.
   */
  @Override
  public void close() {
    // Do nothing
  }
}
