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
 * A class that represents an entry in a PMTiles file.
 */
public class Entry {
  private long tileId;
  private long offset;
  private long length;
  private long runLength;

  /**
   * Creates a new Entry with default values.
   * <p>
   * Use {@link Builder} for a more fluent way to create Entry objects.
   */
  private Entry() {}

  /**
   * Creates a new Entry with the specified values.
   * <p>
   * Consider using {@link Builder} for a more fluent creation approach.
   *
   * @param tileId the tile ID
   * @param offset the offset within the tile data section
   * @param length the length of the tile data
   * @param runLength the run length for compressed entries
   */
  private Entry(long tileId, long offset, long length, long runLength) {
    this.tileId = tileId;
    this.offset = offset;
    this.length = length;
    this.runLength = runLength;
  }

  /**
   * Creates a new Entry from a Builder.
   *
   * @param builder the builder to use
   */
  private Entry(Builder builder) {
    this.tileId = builder.tileId;
    this.offset = builder.offset;
    this.length = builder.length;
    this.runLength = builder.runLength;
  }

  /**
   * Creates a new Builder for Entry objects.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public long getTileId() {
    return tileId;
  }

  public void setTileId(long tileId) {
    this.tileId = tileId;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }

  public long getRunLength() {
    return runLength;
  }

  public void setRunLength(long runLength) {
    this.runLength = runLength;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Entry entry = (Entry) o;
    return tileId == entry.tileId &&
        offset == entry.offset &&
        length == entry.length &&
        runLength == entry.runLength;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tileId, offset, length, runLength);
  }

  /**
   * Builder for Entry objects.
   */
  public static class Builder {
    private long tileId;
    private long offset;
    private long length;
    private long runLength;

    /**
     * Creates a new Builder with default values.
     */
    private Builder() {
      // Use static factory method
    }

    /**
     * Sets the tile ID.
     *
     * @param tileId the tile ID
     * @return this builder
     */
    public Builder tileId(long tileId) {
      this.tileId = tileId;
      return this;
    }

    /**
     * Sets the offset within the tile data section.
     *
     * @param offset the offset
     * @return this builder
     */
    public Builder offset(long offset) {
      this.offset = offset;
      return this;
    }

    /**
     * Sets the length of the tile data.
     *
     * @param length the length
     * @return this builder
     */
    public Builder length(long length) {
      this.length = length;
      return this;
    }

    /**
     * Sets the run length for compressed entries.
     *
     * @param runLength the run length
     * @return this builder
     */
    public Builder runLength(long runLength) {
      this.runLength = runLength;
      return this;
    }

    /**
     * Builds a new Entry object.
     *
     * @return a new Entry
     */
    public Entry build() {
      return new Entry(this);
    }
  }
}
