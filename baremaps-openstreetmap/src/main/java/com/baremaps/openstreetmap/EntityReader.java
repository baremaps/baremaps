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

package com.baremaps.openstreetmap;

import java.io.InputStream;
import java.util.stream.Stream;

/**
 * Reads an OpenStreetMap file as an ordered stream of objects.
 *
 * @param <T> the type of the streamed objects
 */
@FunctionalInterface
public interface EntityReader<T> {

  /**
   * Creates an ordered stream from the provided input. The stream is lazy: the input must stay open
   * until the stream has been consumed.
   *
   * @param input the file content
   * @return an ordered stream
   */
  Stream<T> read(InputStream input);
}
