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

package com.baremaps.pmtiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The fixed-size header that opens a PMTiles archive.
 * <p>
 * The order of the components below is the order of the fields on disk, and {@link #writeTo} and
 * {@link #readFrom} follow it exactly. Keeping the two in step is the only thing that makes this
 * class readable, so new fields belong at the position the specification gives them.
 *
 * @param specVersion the version of the specification the archive conforms to
 * @param rootDirectoryOffset the offset of the root directory, from the start of the file
 * @param rootDirectoryLength the length in bytes of the root directory
 * @param jsonMetadataOffset the offset of the JSON metadata, from the start of the file
 * @param jsonMetadataLength the length in bytes of the JSON metadata
 * @param leafDirectoryOffset the offset of the leaf directory section, from the start of the file
 * @param leafDirectoryLength the length in bytes of the leaf directory section
 * @param tileDataOffset the offset of the tile data section, from the start of the file
 * @param tileDataLength the length in bytes of the tile data section
 * @param numAddressedTiles the number of tiles the archive addresses before run-length encoding, or
 *        zero when unknown
 * @param numTileEntries the number of directory entries with a run length greater than zero, or
 *        zero when unknown
 * @param numTileContents the number of distinct blobs in the tile data section, or zero when
 *        unknown
 * @param clustered whether the tile data section is ordered by tile id, which lets readers coalesce
 *        neighbouring tiles into a single range request
 * @param internalCompression the compression applied to the directories and the JSON metadata
 * @param tileCompression the compression applied to each tile blob
 * @param tileType the format of the tile blobs
 * @param minZoom the lowest zoom level present in the archive
 * @param maxZoom the highest zoom level present in the archive
 * @param minLon the western edge of the bounds, in degrees
 * @param minLat the southern edge of the bounds, in degrees
 * @param maxLon the eastern edge of the bounds, in degrees
 * @param maxLat the northern edge of the bounds, in degrees
 * @param centerZoom the zoom level a viewer should open the archive at
 * @param centerLon the longitude a viewer should centre on, in degrees
 * @param centerLat the latitude a viewer should centre on, in degrees
 */
public record Header(
    int specVersion,
    long rootDirectoryOffset,
    long rootDirectoryLength,
    long jsonMetadataOffset,
    long jsonMetadataLength,
    long leafDirectoryOffset,
    long leafDirectoryLength,
    long tileDataOffset,
    long tileDataLength,
    long numAddressedTiles,
    long numTileEntries,
    long numTileContents,
    boolean clustered,
    Compression internalCompression,
    Compression tileCompression,
    TileType tileType,
    int minZoom,
    int maxZoom,
    double minLon,
    double minLat,
    double maxLon,
    double maxLat,
    int centerZoom,
    double centerLon,
    double centerLat) {

  /** The header occupies a fixed 127 bytes, so the root directory can always follow it. */
  static final int LENGTH = 127;

  /** Identifies the file as a PMTiles archive: the ASCII bytes of "PMTiles". */
  private static final byte[] MAGIC = {0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73};

  /** Coordinates are stored as integers scaled by 1e7, giving roughly centimetre precision. */
  private static final double COORDINATE_SCALE = 1e7;

  /** Returns a builder holding the defaults of a world-wide vector archive. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Writes this header as the 127 bytes that open an archive.
   *
   * @param output the stream to write to
   * @throws IOException if an I/O error occurs
   */
  void writeTo(OutputStream output) throws IOException {
    var buffer = ByteBuffer.allocate(LENGTH).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(MAGIC);
    buffer.put((byte) specVersion);
    buffer.putLong(rootDirectoryOffset);
    buffer.putLong(rootDirectoryLength);
    buffer.putLong(jsonMetadataOffset);
    buffer.putLong(jsonMetadataLength);
    buffer.putLong(leafDirectoryOffset);
    buffer.putLong(leafDirectoryLength);
    buffer.putLong(tileDataOffset);
    buffer.putLong(tileDataLength);
    buffer.putLong(numAddressedTiles);
    buffer.putLong(numTileEntries);
    buffer.putLong(numTileContents);
    buffer.put((byte) (clustered ? 1 : 0));
    buffer.put((byte) internalCompression.ordinal());
    buffer.put((byte) tileCompression.ordinal());
    buffer.put((byte) tileType.ordinal());
    buffer.put((byte) minZoom);
    buffer.put((byte) maxZoom);
    buffer.putInt(scale(minLon));
    buffer.putInt(scale(minLat));
    buffer.putInt(scale(maxLon));
    buffer.putInt(scale(maxLat));
    buffer.put((byte) centerZoom);
    buffer.putInt(scale(centerLon));
    buffer.putInt(scale(centerLat));
    output.write(buffer.array());
  }

  /**
   * Reads the 127 bytes that open an archive.
   *
   * @param input the stream to read from
   * @return the header
   * @throws IOException if the stream is truncated or does not hold a PMTiles header
   */
  static Header readFrom(InputStream input) throws IOException {
    var bytes = input.readNBytes(LENGTH);
    if (bytes.length != LENGTH) {
      throw new IOException("Truncated PMTiles header");
    }
    var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    var magic = new byte[MAGIC.length];
    buffer.get(magic);
    if (!Arrays.equals(magic, MAGIC)) {
      throw new IOException("Invalid PMTiles header magic bytes");
    }
    return new Header(
        buffer.get(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.getLong(),
        buffer.get() == 1,
        Compression.forHeaderValue(buffer.get()),
        Compression.forHeaderValue(buffer.get()),
        TileType.forHeaderValue(buffer.get()),
        buffer.get(),
        buffer.get(),
        unscale(buffer.getInt()),
        unscale(buffer.getInt()),
        unscale(buffer.getInt()),
        unscale(buffer.getInt()),
        buffer.get(),
        unscale(buffer.getInt()),
        unscale(buffer.getInt()));
  }

  private static int scale(double degrees) {
    return (int) (degrees * COORDINATE_SCALE);
  }

  private static double unscale(int scaled) {
    return scaled / COORDINATE_SCALE;
  }

  /**
   * Accumulates the fields of a header.
   * <p>
   * This is the one place the defaults of a new archive are defined; {@link PMTilesWriter} builds
   * on them rather than repeating them.
   */
  public static class Builder {

    private int specVersion = 3;
    private long rootDirectoryOffset;
    private long rootDirectoryLength;
    private long jsonMetadataOffset;
    private long jsonMetadataLength;
    private long leafDirectoryOffset;
    private long leafDirectoryLength;
    private long tileDataOffset;
    private long tileDataLength;
    private long numAddressedTiles;
    private long numTileEntries;
    private long numTileContents;
    private boolean clustered;
    private Compression internalCompression = Compression.GZIP;
    private Compression tileCompression = Compression.GZIP;
    private TileType tileType = TileType.MVT;
    private int minZoom;
    private int maxZoom = 14;
    private double minLon = -180;
    private double minLat = -90;
    private double maxLon = 180;
    private double maxLat = 90;
    private int centerZoom = 3;
    private double centerLon;
    private double centerLat;

    private Builder() {
      // Use Header.builder().
    }

    public Builder specVersion(int specVersion) {
      this.specVersion = specVersion;
      return this;
    }

    public Builder rootDirectoryOffset(long rootDirectoryOffset) {
      this.rootDirectoryOffset = rootDirectoryOffset;
      return this;
    }

    public Builder rootDirectoryLength(long rootDirectoryLength) {
      this.rootDirectoryLength = rootDirectoryLength;
      return this;
    }

    public Builder jsonMetadataOffset(long jsonMetadataOffset) {
      this.jsonMetadataOffset = jsonMetadataOffset;
      return this;
    }

    public Builder jsonMetadataLength(long jsonMetadataLength) {
      this.jsonMetadataLength = jsonMetadataLength;
      return this;
    }

    public Builder leafDirectoryOffset(long leafDirectoryOffset) {
      this.leafDirectoryOffset = leafDirectoryOffset;
      return this;
    }

    public Builder leafDirectoryLength(long leafDirectoryLength) {
      this.leafDirectoryLength = leafDirectoryLength;
      return this;
    }

    public Builder tileDataOffset(long tileDataOffset) {
      this.tileDataOffset = tileDataOffset;
      return this;
    }

    public Builder tileDataLength(long tileDataLength) {
      this.tileDataLength = tileDataLength;
      return this;
    }

    public Builder numAddressedTiles(long numAddressedTiles) {
      this.numAddressedTiles = numAddressedTiles;
      return this;
    }

    public Builder numTileEntries(long numTileEntries) {
      this.numTileEntries = numTileEntries;
      return this;
    }

    public Builder numTileContents(long numTileContents) {
      this.numTileContents = numTileContents;
      return this;
    }

    public Builder clustered(boolean clustered) {
      this.clustered = clustered;
      return this;
    }

    public Builder internalCompression(Compression internalCompression) {
      this.internalCompression = internalCompression;
      return this;
    }

    public Builder tileCompression(Compression tileCompression) {
      this.tileCompression = tileCompression;
      return this;
    }

    public Builder tileType(TileType tileType) {
      this.tileType = tileType;
      return this;
    }

    public Builder zoomRange(int minZoom, int maxZoom) {
      this.minZoom = minZoom;
      this.maxZoom = maxZoom;
      return this;
    }

    public Builder bounds(double minLon, double minLat, double maxLon, double maxLat) {
      this.minLon = minLon;
      this.minLat = minLat;
      this.maxLon = maxLon;
      this.maxLat = maxLat;
      return this;
    }

    public Builder center(int centerZoom, double centerLon, double centerLat) {
      this.centerZoom = centerZoom;
      this.centerLon = centerLon;
      this.centerLat = centerLat;
      return this;
    }

    public Header build() {
      return new Header(specVersion, rootDirectoryOffset, rootDirectoryLength, jsonMetadataOffset,
          jsonMetadataLength, leafDirectoryOffset, leafDirectoryLength, tileDataOffset,
          tileDataLength, numAddressedTiles, numTileEntries, numTileContents, clustered,
          internalCompression, tileCompression, tileType, minZoom, maxZoom, minLon, minLat, maxLon,
          maxLat, centerZoom, centerLon, centerLat);
    }
  }
}
