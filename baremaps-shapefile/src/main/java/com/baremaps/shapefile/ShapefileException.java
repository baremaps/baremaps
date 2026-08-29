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

package com.baremaps.shapefile;

import java.io.IOException;

/**
 * Thrown when a shapefile does not hold what the specification says it should. It extends
 * {@link IOException} because a caller reading a file has no more recourse against a file that is
 * malformed than against one that cannot be read.
 */
public class ShapefileException extends IOException {

  public ShapefileException(String message) {
    super(message);
  }

  public ShapefileException(String message, Throwable cause) {
    super(message, cause);
  }
}
