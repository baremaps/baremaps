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

import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStoreException;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import com.baremaps.tilestore.vector.VectorTileMerger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A {@code TileStore} that produces the shading and the elevation contours of a tile together, as
 * the {@code hillshade} and {@code contour} layers of one vector tile.
 *
 * <p>
 * They are one tile rather than two sources because they are one computation. Both are traced from
 * the same elevation grid, which is the expensive part of producing either: reading it once halves
 * the work on the server and guarantees that the shading and the contours of a tile were derived
 * from the same elevation rather than from two reads that a changing archive could separate. Those
 * layers then travel with the ones the database answers with, which {@link VectorTileMerger} does.
 *
 * <p>
 * The tracing is defined over a range of zoom levels, and a tile outside it holds no terrain rather
 * than terrain traced at a scale it says nothing at: a hillshade of a continent is noise, and the
 * levels below the range are where most of the world's tiles are.
 */
public class VectorTerrainTileStore extends RasterTileStore<ByteBuffer> {

  private final int minzoom;

  private final int maxzoom;

  /**
   * Constructs a {@code VectorTerrainTileStore} with the specified elevation reader.
   *
   * @param elevation the elevation reader
   * @param minzoom the lowest zoom level the terrain is traced at
   * @param maxzoom the highest zoom level it is traced at
   */
  public VectorTerrainTileStore(ElevationReader elevation, int minzoom, int maxzoom) {
    super(elevation);
    this.minzoom = minzoom;
    this.maxzoom = maxzoom;
  }

  /**
   * Opens the tiles a terrain declaration is traced from: the elevation archive it names, read as
   * elevation and traced as the tiles are asked for.
   *
   * <p>
   * The archive is opened once and kept, even by the development server, which rebuilds everything
   * else on every request. Reopening it would reread its directories to answer one tile, and an
   * elevation archive is the one input an edit to the map cannot change the meaning of.
   *
   * @param terrain the terrain the map declares
   * @return the tile store
   * @throws TileStoreException if the archive cannot be opened
   */
  public static VectorTerrainTileStore of(MapSpec.Terrain terrain) throws TileStoreException {
    var dem = Path.of(terrain.dem());
    // A map that declares terrain and has no elevation to trace it from is refused rather than
    // served without relief, which would look like a styling problem and be a missing file.
    if (!Files.exists(dem)) {
      throw new TileStoreException(String.format(
          "The map declares terrain traced from '%s', which does not exist. "
              + "Download an archive of terrarium tiles to that path, "
              + "or remove the terrain block from the map.",
          dem));
    }
    var archive = new PMTilesStore(dem);
    // How deep the archive goes is something it states, so a map that does not say reads it rather
    // than repeating it. A map that says more than the archive holds is refused: the levels past
    // it hold no tiles, and a tile an archive does not hold reads as sea level, which would arrive
    // as a mountain range quietly flattening out.
    var demMaxzoom = terrain.demMaxzoom() == null ? archive.maxzoom() : terrain.demMaxzoom();
    if (demMaxzoom > archive.maxzoom()) {
      throw new TileStoreException(String.format(
          "The map reads the terrain archive '%s' down to zoom %d, and it holds nothing "
              + "below zoom %d. Remove 'terrain.demMaxzoom', which then follows the archive, "
              + "or extract the archive deeper.",
          dem, demMaxzoom, archive.maxzoom()));
    }
    var elevation = new TerrariumElevationReader(archive, terrain.demTileSize(), demMaxzoom);
    return new VectorTerrainTileStore(elevation, terrain.minzoom(), terrain.maxzoom());
  }

  /**
   * Reads the shading and the contours of the specified tile coordinate, or nothing where the
   * terrain is not traced.
   *
   * @param tileCoord the tile coordinate
   * @return the vector tile
   * @throws TileStoreException if an error occurs
   */
  @Override
  public ByteBuffer read(TileCoord tileCoord) throws TileStoreException {
    if (tileCoord.z() < minzoom || tileCoord.z() > maxzoom) {
      return null;
    }
    try {
      var grid = grid(tileCoord);
      return encodeLayers(List.of(
          VectorHillshadeTileStore.layer(grid, tileCoord.z()),
          VectorContourTileStore.layer(grid, tileCoord.z())));
    } catch (TileStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }
}
