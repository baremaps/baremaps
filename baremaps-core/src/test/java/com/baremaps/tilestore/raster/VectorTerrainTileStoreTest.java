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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.dem.ElevationUtils;
import com.baremaps.maplibre.vectortile.VectorTileDecoder;
import com.baremaps.pmtiles.Compression;
import com.baremaps.pmtiles.PMTilesWriter;
import com.baremaps.pmtiles.TileType;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.pmtiles.PMTilesStore;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * The whole chain a terrain tile comes down: an archive of terrarium raster tiles on disk, read as
 * elevation, shaded and contoured into one vector tile.
 *
 * <p>
 * The archive here holds PNG rather than the WebP the published archives ship, because the image
 * format is {@link ImageIO}'s business and everything else on the way is baremaps'.
 */
class VectorTerrainTileStoreTest {

  private static final int TILE_SIZE = 256;

  private static final int ZOOM = 12;

  @TempDir
  Path directory;

  /**
   * A hill rising four thousand meters over a tile ten kilometers across, which is a real mountain
   * rather than a gentle swell: a slope has to be steep enough relative to the size of a pixel for
   * the shading to reach its first level at all.
   */
  @Test
  void tracesAHillIntoShadingAndContours() throws Exception {
    var archive = directory.resolve("terrain.pmtiles");
    write(archive);

    try (var tiles = new PMTilesStore(archive);
        var elevation = new TerrariumElevationReader(tiles, TILE_SIZE, ZOOM);
        var store = new VectorTerrainTileStore(elevation, ZOOM, ZOOM + 1)) {
      // The tile below the one the archive holds, so that the source is read at its own resolution.
      var tile = decode(store.read(new TileCoord(2, 2, ZOOM + 1)));

      // The terrain is traced over the zoom levels it is declared for and nowhere else: a tile
      // outside them carries the rest of the map and no relief.
      assertNull(store.read(new TileCoord(1, 1, ZOOM - 1)));
      assertNull(store.read(new TileCoord(4, 4, ZOOM + 2)));

      var names = tile.getLayers().stream().map(layer -> layer.getName()).toList();
      assertEquals(List.of("hillshade", "contour"), names);

      var contour = tile.getLayers().get(1);
      assertFalse(contour.getFeatures().isEmpty(), "the hill crosses several contours");
      for (var feature : contour.getFeatures()) {
        var level = Integer.parseInt((String) feature.getTags().get("level"));
        assertEquals(0, level % 200, "zoom " + (ZOOM + 1) + " contours every 200 meters");
        // Every fifth contour is marked for the style, which at this interval is every 1000 meters.
        assertEquals(level % 1000 == 0, feature.getTags().containsKey("index"), "level " + level);
      }

      var hillshade = tile.getLayers().get(0);
      assertFalse(hillshade.getFeatures().isEmpty(), "a slope is lit on one side");
      for (var feature : hillshade.getFeatures()) {
        var level = Integer.parseInt((String) feature.getTags().get("level"));
        assertTrue(level >= 1 && level <= 6, "one of the six shading levels, not " + level);
      }
    }
  }

  /**
   * Past the depth of the archive, the grid a tile is traced from stops getting finer.
   *
   * <p>
   * There is nothing further to read: every extra sample is interpolated from the same values, and
   * tracing it draws the shape of the archive's pixels onto the hill. Halving the grid with every
   * level instead keeps the tracing at the resolution the data has, which is the relief a client
   * stretching a shallower tile used to show.
   */
  @Test
  void stopsRefiningTheGridPastTheArchive() throws Exception {
    var archive = directory.resolve("terrain.pmtiles");
    write(archive);

    try (var tiles = new PMTilesStore(archive);
        var elevation = new TerrariumElevationReader(tiles, TILE_SIZE, ZOOM);
        var store = new VectorTerrainTileStore(elevation, 0, 20)) {
      // At and above the level the archive holds, the whole grid is worth asking for.
      assertEquals(TILE_SIZE, store.grid(new TileCoord(1, 1, ZOOM)).size());
      assertEquals(TILE_SIZE, store.grid(new TileCoord(2, 2, ZOOM + 1)).size());
      // Below it, the archive halves what it has to say per tile, and so does the grid.
      assertEquals(TILE_SIZE / 2, store.grid(new TileCoord(4, 4, ZOOM + 2)).size());
      assertEquals(TILE_SIZE / 4, store.grid(new TileCoord(8, 8, ZOOM + 3)).size());
      // The border covers the same ground whatever the grid is.
      assertEquals(4, store.grid(new TileCoord(8, 8, ZOOM + 3)).buffer());
    }
  }

  /** Writes an archive holding one tile: a cone rising to 4000 meters at its centre. */
  private void write(Path path) throws Exception {
    var image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < TILE_SIZE; y++) {
      for (int x = 0; x < TILE_SIZE; x++) {
        var dx = (x - TILE_SIZE / 2.0) / (TILE_SIZE / 2.0);
        var dy = (y - TILE_SIZE / 2.0) / (TILE_SIZE / 2.0);
        var elevation = Math.max(0, 4000 * (1 - Math.hypot(dx, dy)));
        image.setRGB(x, y, ElevationUtils.elevationToTerrarium(elevation));
      }
    }
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);

    try (var writer = new PMTilesWriter(path, Compression.NONE, TileType.PNG)) {
      writer.setZoomRange(ZOOM, ZOOM);
      writer.setBounds(-180, -85, 180, 85);
      writer.setCenter(ZOOM, 0, 0);
      writer.setTile(ZOOM, 1, 1, bytes.toByteArray());
      writer.write();
    }
  }

  /** The tiles a store returns are gzipped, as the tile protocol expects. */
  private static com.baremaps.maplibre.vectortile.Tile decode(ByteBuffer blob) throws Exception {
    var bytes = new byte[blob.remaining()];
    blob.get(bytes);
    try (var gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(bytes))) {
      return new VectorTileDecoder().decodeTile(ByteBuffer.wrap(gzip.readAllBytes()));
    }
  }

  /**
   * A polygon too thin to survive the tile grid is not emitted.
   *
   * <p>
   * Tracing real elevation produces slivers a fraction of a tile unit across. Rounded onto the
   * grid, this one lies flat on a single row: it encloses nothing, so there is nothing to draw and
   * nothing for a decoder to tell an outline from a hole by.
   */
  @Test
  void dropsAPolygonThatRoundsToNothing() {
    var factory = new GeometryFactory();
    var sliver = factory.createPolygon(new Coordinate[] {
        new Coordinate(4319.954995094089, 592),
        new Coordinate(4320, 592.2964781021552),
        new Coordinate(4325.690932311565, 592),
        new Coordinate(4320, 591.6302258912488),
        new Coordinate(4319.954995094089, 592)
    });
    var visible = factory.createPolygon(new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(10, 0),
        new Coordinate(10, 10),
        new Coordinate(0, 0)
    });

    assertFalse(RasterTileStore.drawable(sliver), "flat on one row once rounded");
    assertTrue(RasterTileStore.drawable(visible));
  }

  /** Keeps the temporary directory honest about what was written. */
  @Test
  void writesAnArchiveThatCanBeReopened() throws Exception {
    var archive = directory.resolve("terrain.pmtiles");
    write(archive);
    assertTrue(Files.size(archive) > 0);
    try (var tiles = new PMTilesStore(archive)) {
      assertTrue(tiles.read(new TileCoord(1, 1, ZOOM)).remaining() > 0);
      assertEquals(null, tiles.read(new TileCoord(0, 0, ZOOM)));
    }
  }
}
