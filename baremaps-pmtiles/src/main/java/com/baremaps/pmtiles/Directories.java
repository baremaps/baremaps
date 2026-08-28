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

import java.util.Arrays;
import java.util.Objects;

/**
 * A class that represents directories in a PMTiles file.
 */
public class Directories {

  private final byte[] root;
  private final byte[] leaves;
  private final int numLeaves;

  /**
   * Constructs a new Directories object.
   *
   * @param root the root directory data
   * @param leaves the leaf directory data
   * @param numLeaves the number of leaves
   */
  private Directories(byte[] root, byte[] leaves, int numLeaves) {
    this.root = root;
    this.leaves = leaves;
    this.numLeaves = numLeaves;
  }

  /**
   * Creates a new Directories object from a Builder.
   *
   * @param builder the builder to use
   */
  private Directories(Builder builder) {
    this.root = builder.root;
    this.leaves = builder.leaves;
    this.numLeaves = builder.numLeaves;
  }

  /**
   * Creates a new Builder for Directories objects.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public byte[] getRoot() {
    return root;
  }

  public byte[] getLeaves() {
    return leaves;
  }

  public int getNumLeaves() {
    return numLeaves;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Directories that = (Directories) o;
    return numLeaves == that.numLeaves &&
        Arrays.equals(root, that.root) &&
        Arrays.equals(leaves, that.leaves);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(numLeaves);
    result = 31 * result + Arrays.hashCode(root);
    result = 31 * result + Arrays.hashCode(leaves);
    return result;
  }

  /**
   * Builder for Directories objects.
   */
  public static class Builder {
    private byte[] root = new byte[0];
    private byte[] leaves = new byte[0];
    private int numLeaves = 0;

    /**
     * Creates a new Builder with default values.
     */
    private Builder() {
      // Use static factory method
    }

    /**
     * Sets the root directory data.
     *
     * @param root the root directory data
     * @return this builder
     */
    public Builder root(byte[] root) {
      this.root = root;
      return this;
    }

    /**
     * Sets the leaf directory data.
     *
     * @param leaves the leaf directory data
     * @return this builder
     */
    public Builder leaves(byte[] leaves) {
      this.leaves = leaves;
      return this;
    }

    /**
     * Sets the number of leaves.
     *
     * @param numLeaves the number of leaves
     * @return this builder
     */
    public Builder numLeaves(int numLeaves) {
      this.numLeaves = numLeaves;
      return this;
    }

    /**
     * Builds a new Directories object.
     *
     * @return a new Directories object
     */
    public Directories build() {
      return new Directories(this);
    }
  }
}
