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

import java.util.Objects;

/**
 * A class that represents the header of a PMTiles file.
 */
public class Header {

  private int specVersion;
  private long rootDirectoryOffset;
  private long rootDirectoryLength;
  private long jsonMetadataOffset;
  private long jsonMetadataLength;
  private long leafDirectoryOffset;
  private long leafDirectoryLength;
  private long tileDataOffset;
  private long tileDataLength;

  /**
   * Number of Addressed Tiles
   * <p>
   * The Number of Addressed Tiles is an 8-byte field specifying the total number of tiles in the
   * PMTiles archive, before RunLength Encoding.
   * <p>
   * A value of 0 indicates that the number is unknown.
   * <p>
   * This field is encoded as a little-endian 64-bit unsigned integer.
   */
  private long numAddressedTiles;

  /**
   * Number of Tile Entries
   * <p>
   * The Number of Tile Entries is an 8-byte field specifying the total number of tile entries:
   * entries where RunLength is greater than 0.
   * <p>
   * A value of 0 indicates that the number is unknown.
   * <p>
   * This field is encoded as a little-endian 64-bit unsigned integer.
   *
   * @return the number of tile entries
   */
  private long numTileEntries;

  /**
   * Number of tile entries.
   * <p>
   * The Number of Tile Contents is an 8-byte field specifying the total number of blobs in the tile
   * data section.
   * <p>
   * A value of 0 indicates that the number is unknown.
   * <p>
   * This field is encoded as a little-endian 64-bit unsigned integer.
   */
  private long numTileContents;
  private boolean clustered;
  private Compression internalCompression;
  private Compression tileCompression;
  private TileType tileType;
  private int minZoom;
  private int maxZoom;
  private double minLon;
  private double minLat;
  private double maxLon;
  private double maxLat;
  private int centerZoom;
  private double centerLon;
  private double centerLat;

  /**
   * Creates a new Header with default values.
   * <p>
   * Use {@link Builder} for a more fluent way to create Header objects.
   */
  private Header() {
    this.specVersion = 3;
  }

  /**
   * Creates a new Header with the specified values.
   * <p>
   * This constructor has many parameters. Consider using {@link Builder} instead.
   */
  @SuppressWarnings("squid:S107")
  public Header(
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
      double minLat, double maxLon,
      double maxLat,
      int centerZoom,
      double centerLon,
      double centerLat) {
    this.specVersion = specVersion;
    this.rootDirectoryOffset = rootDirectoryOffset;
    this.rootDirectoryLength = rootDirectoryLength;
    this.jsonMetadataOffset = jsonMetadataOffset;
    this.jsonMetadataLength = jsonMetadataLength;
    this.leafDirectoryOffset = leafDirectoryOffset;
    this.leafDirectoryLength = leafDirectoryLength;
    this.tileDataOffset = tileDataOffset;
    this.tileDataLength = tileDataLength;
    this.numAddressedTiles = numAddressedTiles;
    this.numTileEntries = numTileEntries;
    this.numTileContents = numTileContents;
    this.clustered = clustered;
    this.internalCompression = internalCompression;
    this.tileCompression = tileCompression;
    this.tileType = tileType;
    this.minZoom = minZoom;
    this.maxZoom = maxZoom;
    this.minLon = minLon;
    this.minLat = minLat;
    this.maxLon = maxLon;
    this.maxLat = maxLat;
    this.centerZoom = centerZoom;
    this.centerLon = centerLon;
    this.centerLat = centerLat;
  }

  /**
   * Creates a new Header from a Builder.
   *
   * @param builder the builder to use
   */
  private Header(Builder builder) {
    this.specVersion = builder.specVersion;
    this.rootDirectoryOffset = builder.rootDirectoryOffset;
    this.rootDirectoryLength = builder.rootDirectoryLength;
    this.jsonMetadataOffset = builder.jsonMetadataOffset;
    this.jsonMetadataLength = builder.jsonMetadataLength;
    this.leafDirectoryOffset = builder.leafDirectoryOffset;
    this.leafDirectoryLength = builder.leafDirectoryLength;
    this.tileDataOffset = builder.tileDataOffset;
    this.tileDataLength = builder.tileDataLength;
    this.numAddressedTiles = builder.numAddressedTiles;
    this.numTileEntries = builder.numTileEntries;
    this.numTileContents = builder.numTileContents;
    this.clustered = builder.clustered;
    this.internalCompression = builder.internalCompression;
    this.tileCompression = builder.tileCompression;
    this.tileType = builder.tileType;
    this.minZoom = builder.minZoom;
    this.maxZoom = builder.maxZoom;
    this.minLon = builder.minLon;
    this.minLat = builder.minLat;
    this.maxLon = builder.maxLon;
    this.maxLat = builder.maxLat;
    this.centerZoom = builder.centerZoom;
    this.centerLon = builder.centerLon;
    this.centerLat = builder.centerLat;
  }

  /**
   * Creates a new Builder for Header objects.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public int getSpecVersion() {
    return specVersion;
  }

  public void setSpecVersion(int specVersion) {
    this.specVersion = specVersion;
  }

  public long getRootDirectoryOffset() {
    return rootDirectoryOffset;
  }

  public void setRootOffset(long rootDirectoryOffset) {
    this.rootDirectoryOffset = rootDirectoryOffset;
  }

  public long getRootDirectoryLength() {
    return rootDirectoryLength;
  }

  public void setRootLength(long rootDirectoryLength) {
    this.rootDirectoryLength = rootDirectoryLength;
  }

  public long getJsonMetadataOffset() {
    return jsonMetadataOffset;
  }

  public void setMetadataOffset(long jsonMetadataOffset) {
    this.jsonMetadataOffset = jsonMetadataOffset;
  }

  public long getJsonMetadataLength() {
    return jsonMetadataLength;
  }

  public void setMetadataLength(long jsonMetadataLength) {
    this.jsonMetadataLength = jsonMetadataLength;
  }

  public long getLeafDirectoryOffset() {
    return leafDirectoryOffset;
  }

  public void setLeavesOffset(long leafDirectoryOffset) {
    this.leafDirectoryOffset = leafDirectoryOffset;
  }

  public long getLeafDirectoryLength() {
    return leafDirectoryLength;
  }

  public void setLeavesLength(long leafDirectoryLength) {
    this.leafDirectoryLength = leafDirectoryLength;
  }

  public long getTileDataOffset() {
    return tileDataOffset;
  }

  public void setTilesOffset(long tileDataOffset) {
    this.tileDataOffset = tileDataOffset;
  }

  public long getTileDataLength() {
    return tileDataLength;
  }

  public void setTilesLength(long tileDataLength) {
    this.tileDataLength = tileDataLength;
  }

  public long getNumAddressedTiles() {
    return numAddressedTiles;
  }

  public void setNumAddressedTiles(long numAddressedTiles) {
    this.numAddressedTiles = numAddressedTiles;
  }

  public long getNumTileEntries() {
    return numTileEntries;
  }

  public void setNumTileEntries(long numTileEntries) {
    this.numTileEntries = numTileEntries;
  }

  public long getNumTileContents() {
    return numTileContents;
  }

  public void setNumTileContents(long numTileContents) {
    this.numTileContents = numTileContents;
  }

  public boolean isClustered() {
    return clustered;
  }

  public void setClustered(boolean clustered) {
    this.clustered = clustered;
  }

  public Compression getInternalCompression() {
    return internalCompression;
  }

  public void setInternalCompression(Compression internalCompression) {
    this.internalCompression = internalCompression;
  }

  public Compression getTileCompression() {
    return tileCompression;
  }

  public void setTileCompression(Compression tileCompression) {
    this.tileCompression = tileCompression;
  }

  public TileType getTileType() {
    return tileType;
  }

  public void setTileType(TileType tileType) {
    this.tileType = tileType;
  }

  public int getMinZoom() {
    return minZoom;
  }

  public void setMinZoom(int minZoom) {
    this.minZoom = minZoom;
  }

  public int getMaxZoom() {
    return maxZoom;
  }

  public void setMaxZoom(int maxZoom) {
    this.maxZoom = maxZoom;
  }

  public double getMinLon() {
    return minLon;
  }

  public void setMinLon(double minLon) {
    this.minLon = minLon;
  }

  public double getMinLat() {
    return minLat;
  }

  public void setMinLat(double minLat) {
    this.minLat = minLat;
  }

  public double getMaxLon() {
    return maxLon;
  }

  public void setMaxLon(double maxLon) {
    this.maxLon = maxLon;
  }

  public double getMaxLat() {
    return maxLat;
  }

  public void setMaxLat(double maxLat) {
    this.maxLat = maxLat;
  }

  public int getCenterZoom() {
    return centerZoom;
  }

  public void setCenterZoom(int centerZoom) {
    this.centerZoom = centerZoom;
  }

  public double getCenterLon() {
    return centerLon;
  }

  public void setCenterLon(double centerLon) {
    this.centerLon = centerLon;
  }

  public double getCenterLat() {
    return centerLat;
  }

  public void setCenterLat(double centerLat) {
    this.centerLat = centerLat;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Header header = (Header) o;
    return specVersion == header.specVersion && rootDirectoryOffset == header.rootDirectoryOffset
        && rootDirectoryLength == header.rootDirectoryLength
        && jsonMetadataOffset == header.jsonMetadataOffset
        && jsonMetadataLength == header.jsonMetadataLength
        && leafDirectoryOffset == header.leafDirectoryOffset
        && leafDirectoryLength == header.leafDirectoryLength
        && tileDataOffset == header.tileDataOffset && tileDataLength == header.tileDataLength
        && numAddressedTiles == header.numAddressedTiles && numTileEntries == header.numTileEntries
        && numTileContents == header.numTileContents && clustered == header.clustered
        && minZoom == header.minZoom && maxZoom == header.maxZoom
        && Double.compare(header.minLon, minLon) == 0 && Double.compare(header.minLat, minLat) == 0
        && Double.compare(header.maxLon, maxLon) == 0 && Double.compare(header.maxLat, maxLat) == 0
        && centerZoom == header.centerZoom && Double.compare(header.centerLon, centerLon) == 0
        && Double.compare(header.centerLat, centerLat) == 0
        && internalCompression == header.internalCompression
        && tileCompression == header.tileCompression && tileType == header.tileType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(specVersion, rootDirectoryOffset, rootDirectoryLength, jsonMetadataOffset,
        jsonMetadataLength, leafDirectoryOffset, leafDirectoryLength, tileDataOffset,
        tileDataLength, numAddressedTiles, numTileEntries, numTileContents, clustered,
        internalCompression, tileCompression, tileType, minZoom, maxZoom, minLon, minLat, maxLon,
        maxLat, centerZoom, centerLon, centerLat);
  }

  /**
   * Builder for Header objects.
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

    /**
     * Creates a new Builder with default values.
     */
    private Builder() {
      // Use static factory method
    }

    /**
     * Sets the spec version.
     *
     * @param specVersion the spec version
     * @return this builder
     */
    public Builder specVersion(int specVersion) {
      this.specVersion = specVersion;
      return this;
    }

    /**
     * Sets the root directory offset.
     *
     * @param rootDirectoryOffset the root directory offset
     * @return this builder
     */
    public Builder rootDirectoryOffset(long rootDirectoryOffset) {
      this.rootDirectoryOffset = rootDirectoryOffset;
      return this;
    }

    /**
     * Sets the root directory length.
     *
     * @param rootDirectoryLength the root directory length
     * @return this builder
     */
    public Builder rootDirectoryLength(long rootDirectoryLength) {
      this.rootDirectoryLength = rootDirectoryLength;
      return this;
    }

    /**
     * Sets the JSON metadata offset.
     *
     * @param jsonMetadataOffset the JSON metadata offset
     * @return this builder
     */
    public Builder jsonMetadataOffset(long jsonMetadataOffset) {
      this.jsonMetadataOffset = jsonMetadataOffset;
      return this;
    }

    /**
     * Sets the JSON metadata length.
     *
     * @param jsonMetadataLength the JSON metadata length
     * @return this builder
     */
    public Builder jsonMetadataLength(long jsonMetadataLength) {
      this.jsonMetadataLength = jsonMetadataLength;
      return this;
    }

    /**
     * Sets the leaf directory offset.
     *
     * @param leafDirectoryOffset the leaf directory offset
     * @return this builder
     */
    public Builder leafDirectoryOffset(long leafDirectoryOffset) {
      this.leafDirectoryOffset = leafDirectoryOffset;
      return this;
    }

    /**
     * Sets the leaf directory length.
     *
     * @param leafDirectoryLength the leaf directory length
     * @return this builder
     */
    public Builder leafDirectoryLength(long leafDirectoryLength) {
      this.leafDirectoryLength = leafDirectoryLength;
      return this;
    }

    /**
     * Sets the tile data offset.
     *
     * @param tileDataOffset the tile data offset
     * @return this builder
     */
    public Builder tileDataOffset(long tileDataOffset) {
      this.tileDataOffset = tileDataOffset;
      return this;
    }

    /**
     * Sets the tile data length.
     *
     * @param tileDataLength the tile data length
     * @return this builder
     */
    public Builder tileDataLength(long tileDataLength) {
      this.tileDataLength = tileDataLength;
      return this;
    }

    /**
     * Sets the number of addressed tiles.
     *
     * @param numAddressedTiles the number of addressed tiles
     * @return this builder
     */
    public Builder numAddressedTiles(long numAddressedTiles) {
      this.numAddressedTiles = numAddressedTiles;
      return this;
    }

    /**
     * Sets the number of tile entries.
     *
     * @param numTileEntries the number of tile entries
     * @return this builder
     */
    public Builder numTileEntries(long numTileEntries) {
      this.numTileEntries = numTileEntries;
      return this;
    }

    /**
     * Sets the number of tile contents.
     *
     * @param numTileContents the number of tile contents
     * @return this builder
     */
    public Builder numTileContents(long numTileContents) {
      this.numTileContents = numTileContents;
      return this;
    }

    /**
     * Sets whether the tiles are clustered.
     *
     * @param clustered whether the tiles are clustered
     * @return this builder
     */
    public Builder clustered(boolean clustered) {
      this.clustered = clustered;
      return this;
    }

    /**
     * Sets the internal compression.
     *
     * @param internalCompression the internal compression
     * @return this builder
     */
    public Builder internalCompression(Compression internalCompression) {
      this.internalCompression = internalCompression;
      return this;
    }

    /**
     * Sets the tile compression.
     *
     * @param tileCompression the tile compression
     * @return this builder
     */
    public Builder tileCompression(Compression tileCompression) {
      this.tileCompression = tileCompression;
      return this;
    }

    /**
     * Sets the tile type.
     *
     * @param tileType the tile type
     * @return this builder
     */
    public Builder tileType(TileType tileType) {
      this.tileType = tileType;
      return this;
    }

    /**
     * Sets the minimum zoom level.
     *
     * @param minZoom the minimum zoom level
     * @return this builder
     */
    public Builder minZoom(int minZoom) {
      this.minZoom = minZoom;
      return this;
    }

    /**
     * Sets the maximum zoom level.
     *
     * @param maxZoom the maximum zoom level
     * @return this builder
     */
    public Builder maxZoom(int maxZoom) {
      this.maxZoom = maxZoom;
      return this;
    }

    /**
     * Sets the minimum longitude.
     *
     * @param minLon the minimum longitude
     * @return this builder
     */
    public Builder minLon(double minLon) {
      this.minLon = minLon;
      return this;
    }

    /**
     * Sets the minimum latitude.
     *
     * @param minLat the minimum latitude
     * @return this builder
     */
    public Builder minLat(double minLat) {
      this.minLat = minLat;
      return this;
    }

    /**
     * Sets the maximum longitude.
     *
     * @param maxLon the maximum longitude
     * @return this builder
     */
    public Builder maxLon(double maxLon) {
      this.maxLon = maxLon;
      return this;
    }

    /**
     * Sets the maximum latitude.
     *
     * @param maxLat the maximum latitude
     * @return this builder
     */
    public Builder maxLat(double maxLat) {
      this.maxLat = maxLat;
      return this;
    }

    /**
     * Sets the center zoom level.
     *
     * @param centerZoom the center zoom level
     * @return this builder
     */
    public Builder centerZoom(int centerZoom) {
      this.centerZoom = centerZoom;
      return this;
    }

    /**
     * Sets the center longitude.
     *
     * @param centerLon the center longitude
     * @return this builder
     */
    public Builder centerLon(double centerLon) {
      this.centerLon = centerLon;
      return this;
    }

    /**
     * Sets the center latitude.
     *
     * @param centerLat the center latitude
     * @return this builder
     */
    public Builder centerLat(double centerLat) {
      this.centerLat = centerLat;
      return this;
    }

    /**
     * Sets the bounds of the tiles.
     *
     * @param minLon the minimum longitude
     * @param minLat the minimum latitude
     * @param maxLon the maximum longitude
     * @param maxLat the maximum latitude
     * @return this builder
     */
    public Builder bounds(double minLon, double minLat, double maxLon, double maxLat) {
      this.minLon = minLon;
      this.minLat = minLat;
      this.maxLon = maxLon;
      this.maxLat = maxLat;
      return this;
    }

    /**
     * Sets the center of the tiles.
     *
     * @param centerZoom the center zoom level
     * @param centerLon the center longitude
     * @param centerLat the center latitude
     * @return this builder
     */
    public Builder center(int centerZoom, double centerLon, double centerLat) {
      this.centerZoom = centerZoom;
      this.centerLon = centerLon;
      this.centerLat = centerLat;
      return this;
    }

    /**
     * Builds a new Header object.
     *
     * @return a new Header
     */
    public Header build() {
      return new Header(this);
    }
  }
}
