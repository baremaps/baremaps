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

package com.baremaps.data.collection;

/**
 * Exception thrown when an error occurs in a data collection operation.
 */
public class DataCollectionException extends RuntimeException {

  /**
   * Constructs a new exception with null as its detail message.
   */
  public DataCollectionException() {}

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public DataCollectionException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause the cause of this exception
   */
  public DataCollectionException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of this exception
   */
  public DataCollectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
