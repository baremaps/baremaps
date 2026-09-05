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

package com.baremaps.cli.map;


import com.baremaps.cli.BaremapsException;
import com.baremaps.config.ConfigReader;
import com.baremaps.maplibre.map.MapCompiler;
import com.baremaps.maplibre.map.MapSpec;
import com.baremaps.maplibre.style.Style;
import com.baremaps.maplibre.tilejson.TileJSON;
import com.baremaps.maplibre.tileset.Tileset;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import com.baremaps.tilestore.raster.VectorTerrainTileStore;
import com.baremaps.tilestore.vector.VectorTileMerger;
import com.baremaps.utils.ObjectMapperUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the style and the tileset a command works with, from either a map specification or the
 * separate files that preceded it.
 *
 * <p>
 * The separate files keep working. A tileset written by hand is still a tileset, and a project that
 * has one has no reason to be broken by the arrival of a format that can derive one.
 */
final class MapInput {

  private MapInput() {}

  /**
   * Checks that the command was given one of the two ways of describing a map, and not both.
   *
   * @param map the map specification, or null
   * @param tileset the tileset file, or null
   * @param style the style file, or null
   */
  static void validate(Path map, Path tileset, Path style) {
    if (map != null && (tileset != null || style != null)) {
      throw new BaremapsException(
          "Pass --map, or --tileset and --style, but not both: the map specification derives them.");
    }
    if (map == null && (tileset == null || style == null)) {
      throw new BaremapsException("Pass --map, or both --tileset and --style.");
    }
  }

  static Tileset tileset(ConfigReader reader, Path map, Path tileset) {
    if (map != null) {
      return MapCompiler.tileset(read(reader, map, MapSpec.class));
    }
    return read(reader, tileset, Tileset.class);
  }

  static Style style(ConfigReader reader, Path map, Path style) {
    if (map != null) {
      return MapCompiler.style(read(reader, map, MapSpec.class));
    }
    return read(reader, style, Style.class);
  }

  /**
   * The terrain the map declares, or null when it declares none or was described as a tileset and a
   * style, which have nowhere to say it.
   */
  static MapSpec.Terrain terrain(ConfigReader reader, Path map) {
    return map == null ? null : read(reader, map, MapSpec.class).terrain();
  }

  /**
   * The tiles the terrain of a map is traced from, or null when it declares none.
   *
   * <p>
   * It is opened once and kept, even by the development server, which rebuilds everything else on
   * every request: an elevation archive is the one input an edit to the map cannot change the
   * meaning of, and reopening it would reread its directories to answer one tile.
   */
  static TileStore<ByteBuffer> terrainTileStore(MapSpec.Terrain terrain) throws TileStoreException {
    return terrain == null ? null : VectorTerrainTileStore.of(terrain);
  }

  /**
   * The tiles a map is served from: the ones its queries answer with, carrying the shading and the
   * contours traced from elevation when it has any.
   *
   * <p>
   * One tile and not two, because the map has one source: a client asks for a place once and is
   * given everything the map draws there. The terrain is traced as it is asked for rather than
   * written into the database the queries read, so the two meet here.
   *
   * @param tiles the tiles the queries answer with
   * @param terrain the tiles the terrain is traced into, or null
   */
  static TileStore<ByteBuffer> tileStore(TileStore<ByteBuffer> tiles,
      TileStore<ByteBuffer> terrain) {
    return terrain == null ? tiles : new VectorTileMerger(List.of(tiles, terrain));
  }

  /**
   * The tileset as it is published to a browser, which is the same document without the queries and
   * the database behind them.
   */
  static TileJSON tileJSON(ConfigReader reader, Path map, Path tileset) {
    if (map != null) {
      return ObjectMapperUtils.objectMapper()
          .convertValue(tileset(reader, map, null), TileJSON.class);
    }
    return read(reader, tileset, TileJSON.class);
  }

  private static <T> T read(ConfigReader reader, Path path, Class<T> type) {
    try {
      return reader.read(path, type);
    } catch (IOException e) {
      throw new BaremapsException(e);
    }
  }
}
