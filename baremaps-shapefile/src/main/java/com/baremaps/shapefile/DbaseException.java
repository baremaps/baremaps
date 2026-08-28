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
 * Thrown when the DBF file seems to be invalid.
 *
 * @author Marc Le Bihan
 */
public class DbaseException extends IOException {

  /**
   * Construct an exception.
   *
   * @param message Message of the exception.
   */
  public DbaseException(String message) {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message Message of the exception.
   * @param cause Root cause of the exception.
   */
  public DbaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
