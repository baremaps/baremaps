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

package com.baremaps.data.type;



import com.baremaps.data.type.PairDataType.Pair;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A {@link DataType} for reading and writing pairs of values in {@link ByteBuffer}s.
 *
 * @param <L> the type of the left value in the pair
 * @param <R> the type of the right value in the pair
 */
public class PairDataType<L, R> extends FixedSizeDataType<Pair<L, R>> {

  private final FixedSizeDataType<L> left;
  private final FixedSizeDataType<R> right;

  /**
   * Constructs a {@link PairDataType} with data types for left and right values.
   *
   * @param left the data type for the left value
   * @param right the data type for the right value
   */
  public PairDataType(final FixedSizeDataType<L> left, final FixedSizeDataType<R> right) {
    super(left.size() + right.size());
    this.left = left;
    this.right = right;
  }

  /** {@inheritDoc} */
  @Override
  public void write(final ByteBuffer buffer, final int position, final Pair<L, R> value) {
    left.write(buffer, position, value.left());
    right.write(buffer, position + left.size(), value.right());
  }

  /** {@inheritDoc} */
  @Override
  public Pair<L, R> read(final ByteBuffer buffer, final int position) {
    return new Pair<>(
        left.read(buffer, position),
        right.read(buffer, position + left.size()));
  }

  /**
   * A class representing a pair of values.
   *
   * @param <L> the type of the left value
   * @param <R> the type of the right value
   */
  public static class Pair<L, R> {

    private final L left;

    private final R right;

    /**
     * Constructs a pair with left and right values.
     *
     * @param left the left value
     * @param right the right value
     */
    public Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }

    /**
     * Returns the left value of the pair.
     *
     * @return the left value
     */
    public L left() {
      return left;
    }

    /**
     * Returns the right value of the pair.
     *
     * @return the right value
     */
    public R right() {
      return right;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Pair<?, ?>pair)) {
        return false;
      }

      if (!Objects.equals(left, pair.left)) {
        return false;
      }

      return Objects.equals(right, pair.right);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      int result = left != null ? left.hashCode() : 0;
      result = 31 * result + (right != null ? right.hashCode() : 0);
      return result;
    }
  }
}
