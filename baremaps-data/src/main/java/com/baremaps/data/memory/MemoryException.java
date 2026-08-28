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

package com.baremaps.data.memory;

/** Exception thrown when a memory operation fails. */
public class MemoryException extends RuntimeException {

  /** Constructs a MemoryException with no message or cause. */
  public MemoryException() {}

  /**
   * Constructs a MemoryException with the specified message.
   *
   * @param message error message
   */
  public MemoryException(String message) {
    super(message);
  }

  /**
   * Constructs a MemoryException with the specified cause.
   *
   * @param cause the underlying cause
   */
  public MemoryException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a MemoryException with the specified message and cause.
   *
   * @param message error message
   * @param cause the underlying cause
   */
  public MemoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
