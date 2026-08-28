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

package com.baremaps.openstreetmap.model;



/** Represents a header block in an OpenStreetMap dataset. */
public final class HeaderBlock extends Block {

  private final Header header;

  private final Bound bound;

  /**
   * Constructs an OpenStreetMap {@code HeaderBlock} with the specified parameters.
   *
   * @param blob the blob
   * @param header the header
   * @param bound the bound
   */
  public HeaderBlock(Blob blob, Header header, Bound bound) {
    super(blob);
    this.header = header;
    this.bound = bound;
  }

  /**
   * Returns the header.
   *
   * @return the header
   */
  public Header getHeader() {
    return header;
  }

  /**
   * Returns the bounds.
   *
   * @return the bounds
   */
  public Bound getBound() {
    return bound;
  }

}
