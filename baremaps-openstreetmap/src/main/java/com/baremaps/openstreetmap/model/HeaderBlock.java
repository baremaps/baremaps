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

import java.util.List;

/**
 * The first block of an OpenStreetMap PBF file, holding its header and bounds.
 *
 * @param header the header of the file
 * @param bound the bounds of the data the file holds
 */
public record HeaderBlock(Header header, Bound bound) implements Block {

  @Override
  public List<Entity> entities() {
    return List.of(header, bound);
  }
}
