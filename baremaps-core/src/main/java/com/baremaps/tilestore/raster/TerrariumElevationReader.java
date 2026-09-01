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

import com.baremaps.dem.ElevationUtils;
import com.baremaps.tilestore.TileCoord;
import com.baremaps.tilestore.TileStore;
import com.baremaps.tilestore.TileStoreException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

/**
 * An {@link ElevationReader} that samples a pyramid of terrarium encoded raster tiles, such as the
 * planet-wide archives published by <a href="https://mapterhorn.com/">Mapterhorn</a>.
 *
 * <p>
 * The pyramid is already in the projection and the tiling scheme the map draws in, so reading it is
 * a resampling within one grid rather than a reprojection: the source level whose pixels are at
 * least as fine as the ones asked for is chosen, and every value is interpolated from the four
 * source pixels around it. That is what makes this cheap enough to trace contours per request,
 * where a GeoTIFF has to be reprojected first.
 *
 * <p>
 * The tiles carry no metadata about themselves, so the encoding is not sniffed: they are terrarium,
 * which is what {@code ElevationUtils.terrariumToElevation} decodes. Their image format is left to
 * {@link ImageIO}, so an archive of WebP tiles needs a WebP reader on the classpath and one of PNG
 * tiles needs nothing.
 *
 * <p>
 * A tile absent from the archive reads as sea level rather than as an error. Terrain archives omit
 * the tiles that hold nothing but ocean, and an archive covering one country is a legitimate way to
 * work on that country.
 */
public class TerrariumElevationReader implements ElevationReader {

  /** The side of a tile in the archives this was written for. */
  public static final int DEFAULT_TILE_SIZE = 512;

  /** How many decoded source tiles are kept. Each holds a float per pixel, so 1 MiB at 512. */
  private static final int DEFAULT_CACHE_SIZE = 64;

  /** Stands for a tile the archive does not hold, so that its absence is cached too. */
  private static final float[] MISSING = new float[0];

  private final TileStore<ByteBuffer> tiles;

  private final int tileSize;

  private final int maxzoom;

  private final Cache<TileCoord, float[]> cache;

  /**
   * Constructs a reader over an archive of terrarium tiles.
   *
   * @param tiles the archive
   * @param tileSize the side of a source tile, in pixels
   * @param maxzoom the deepest level the archive holds, below which it is sampled beyond its
   *        resolution
   */
  public TerrariumElevationReader(TileStore<ByteBuffer> tiles, int tileSize, int maxzoom) {
    this(tiles, tileSize, maxzoom, DEFAULT_CACHE_SIZE);
  }

  /**
   * Constructs a reader over an archive of terrarium tiles.
   *
   * @param tiles the archive
   * @param tileSize the side of a source tile, in pixels
   * @param maxzoom the deepest level the archive holds
   * @param cacheSize how many decoded source tiles to keep
   */
  public TerrariumElevationReader(TileStore<ByteBuffer> tiles, int tileSize, int maxzoom,
      int cacheSize) {
    if (Integer.bitCount(tileSize) != 1) {
      throw new IllegalArgumentException("A tile size must be a power of two, not " + tileSize);
    }
    this.tiles = tiles;
    this.tileSize = tileSize;
    this.maxzoom = maxzoom;
    this.cache = Caffeine.newBuilder().maximumSize(cacheSize).build();
  }

  /**
   * Reads the elevation over the extent of a tile, widened by a border.
   *
   * @param tileCoord the tile coordinate
   * @param tileSizePx the side of the tile, in pixels
   * @param tileBufferPx the border added on every side, in pixels
   * @return the elevation in meters, row-major
   * @throws TileStoreException if the archive cannot be read
   */
  @Override
  public double[] read(TileCoord tileCoord, int tileSizePx, int tileBufferPx)
      throws TileStoreException {
    if (Integer.bitCount(tileSizePx) != 1) {
      throw new IllegalArgumentException("A tile size must be a power of two, not " + tileSizePx);
    }
    var gridSize = tileSizePx + 2 * tileBufferPx;

    // The level whose pixels are at least as fine as the ones asked for. Sampling a coarser level
    // would invent detail; a finer one would be read and thrown away.
    var zoom = Math.clamp(
        tileCoord.z() + log2(tileSizePx) - log2(tileSize), 0, maxzoom);
    var world = (long) tileSize << zoom;

    // Where the requested grid falls in the source level, in source pixels. The half pixel is the
    // offset between a pixel's corner and its centre, which is what is being sampled.
    var scale = (double) world / ((long) tileSizePx << tileCoord.z());
    var originX = ((long) tileCoord.x() * tileSizePx - tileBufferPx + 0.5) * scale - 0.5;
    var originY = ((long) tileCoord.y() * tileSizePx - tileBufferPx + 0.5) * scale - 0.5;

    // Bilinear interpolation reads the pixel below and to the right of the last sample, so the
    // window is one wider than the samples span.
    var window = new Window(
        Math.floorDiv((long) Math.floor(originX), tileSize),
        Math.floorDiv((long) Math.floor(originY), tileSize),
        Math.floorDiv((long) Math.floor(originX + (gridSize - 1) * scale) + 1, tileSize),
        Math.floorDiv((long) Math.floor(originY + (gridSize - 1) * scale) + 1, tileSize),
        zoom, world);

    var grid = new double[gridSize * gridSize];
    for (int j = 0; j < gridSize; j++) {
      var sourceY = originY + j * scale;
      var y0 = (long) Math.floor(sourceY);
      var fy = sourceY - y0;
      for (int i = 0; i < gridSize; i++) {
        var sourceX = originX + i * scale;
        var x0 = (long) Math.floor(sourceX);
        var fx = sourceX - x0;
        var top = window.sample(x0, y0) * (1 - fx) + window.sample(x0 + 1, y0) * fx;
        var bottom = window.sample(x0, y0 + 1) * (1 - fx) + window.sample(x0 + 1, y0 + 1) * fx;
        grid[j * gridSize + i] = top * (1 - fy) + bottom * fy;
      }
    }
    return grid;
  }

  /**
   * The source tiles one grid is read from, held for the length of that read.
   *
   * <p>
   * A grid spans a handful of tiles, and every one of its pixels samples four values from them.
   * Resolving each of those through the cache would be a hundred thousand lookups for four tiles,
   * so the window is resolved once and indexed arithmetically afterwards.
   */
  private final class Window {

    private final long minX;
    private final long minY;
    private final int columns;
    private final int rows;
    private final long world;
    private final float[][] grids;

    private Window(long minX, long minY, long maxX, long maxY, int zoom, long world)
        throws TileStoreException {
      // The world wraps around in x, so a window crossing the antimeridian keeps counting and the
      // column is wrapped when the tile is fetched. It does not wrap in y: a grid reaching past a
      // pole repeats the last row, which is the only elevation there is.
      var lastTile = world / tileSize - 1;
      this.world = world;
      this.minX = minX;
      this.minY = Math.clamp(minY, 0, lastTile);
      this.columns = (int) (maxX - minX + 1);
      this.rows = (int) (Math.clamp(maxY, 0, lastTile) - this.minY + 1);
      this.grids = new float[rows * columns][];
      for (int row = 0; row < rows; row++) {
        for (int column = 0; column < columns; column++) {
          var x = Math.floorMod(minX + column, lastTile + 1);
          grids[row * columns + column] =
              tile(new TileCoord((int) x, (int) (this.minY + row), zoom));
        }
      }
    }

    /**
     * The elevation at a pixel of the source level. A pixel beyond a pole is read as the nearest
     * one inside it; a pixel beyond the antimeridian wraps around.
     */
    private double sample(long x, long y) {
      y = Math.clamp(y, 0, world - 1);
      var column = (int) Math.clamp(Math.floorDiv(x, tileSize) - minX, 0, columns - 1);
      var row = (int) Math.clamp(Math.floorDiv(y, tileSize) - minY, 0, rows - 1);
      var grid = grids[row * columns + column];
      if (grid == MISSING) {
        return 0;
      }
      var px = (int) Math.floorMod(x, (long) tileSize);
      var py = (int) Math.floorMod(y, (long) tileSize);
      return grid[py * tileSize + px];
    }
  }

  /** Decodes a source tile, or returns {@link #MISSING} when the archive does not hold it. */
  private float[] tile(TileCoord tileCoord) throws TileStoreException {
    var cached = cache.getIfPresent(tileCoord);
    if (cached != null) {
      return cached;
    }
    var decoded = decode(tiles.read(tileCoord));
    cache.put(tileCoord, decoded);
    return decoded;
  }

  private float[] decode(ByteBuffer blob) throws TileStoreException {
    if (blob == null) {
      return MISSING;
    }
    try {
      var bytes = new byte[blob.remaining()];
      blob.duplicate().get(bytes);
      var image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null) {
        throw new TileStoreException(
            "No image reader for the tiles of this archive. WebP tiles need a WebP reader.");
      }
      if (image.getWidth() != tileSize || image.getHeight() != tileSize) {
        throw new TileStoreException(String.format(
            "A tile of this archive is %dx%d, and the reader was told %d.",
            image.getWidth(), image.getHeight(), tileSize));
      }
      var grid = new float[tileSize * tileSize];
      for (int y = 0; y < tileSize; y++) {
        for (int x = 0; x < tileSize; x++) {
          grid[y * tileSize + x] =
              (float) ElevationUtils.terrariumToElevation(image.getRGB(x, y));
        }
      }
      return grid;
    } catch (TileStoreException e) {
      throw e;
    } catch (Exception e) {
      throw new TileStoreException(e);
    }
  }

  private static int log2(int value) {
    return Integer.numberOfTrailingZeros(value);
  }

  /** Closes the archive the elevation is read from. */
  @Override
  public void close() throws Exception {
    tiles.close();
  }
}
