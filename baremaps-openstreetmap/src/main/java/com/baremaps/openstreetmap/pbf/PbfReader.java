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

package com.baremaps.openstreetmap.pbf;



import com.baremaps.openstreetmap.OpenStreetMapFormat.EntityReader;

public interface PbfReader<T> extends EntityReader<T> {

  /**
   * Gets the number of blobs buffered by the parser to parallelize deserialization.
   *
   * @return the size of the buffer
   */
  int getBuffer();

  /**
   * Sets the number of blobs buffered by the parser to parallelize deserialization.
   *
   * @param buffer the size of the buffer
   * @return the reader
   */
  PbfReader<T> setBuffer(int buffer);

}
