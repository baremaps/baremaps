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

/**
 * Generic serializer interface for PMTiles components.
 *
 * @param <T> the type of object to serialize/deserialize
 */
public interface Serializer<T> {

  /**
   * Serialize an object to an output stream.
   *
   * @param object the object to serialize
   * @param output the output stream to write to
   * @throws IOException if an I/O error occurs
   */
  void serialize(T object, OutputStream output) throws IOException;

  /**
   * Deserialize an object from an input stream.
   *
   * @param input the input stream to read from
   * @return the deserialized object
   * @throws IOException if an I/O error occurs
   */
  T deserialize(InputStream input) throws IOException;
}
