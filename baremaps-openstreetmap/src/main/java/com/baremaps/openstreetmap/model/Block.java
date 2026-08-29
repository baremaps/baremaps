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
 * A block of an OpenStreetMap PBF file. Blocks are the unit of parallel decoding: each one is
 * self-contained and the entities it holds can be listed in file order.
 */
public sealed

interface Block
permits HeaderBlock, DataBlock
{

  /**
   * Returns the entities of the block in file order.
   *
   * @return the entities
   */
  List<Entity> entities();
}
