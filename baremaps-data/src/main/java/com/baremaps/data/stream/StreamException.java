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

package com.baremaps.data.stream;

/**
 * When a checked exception occurs in a stream, it is a good practice to wrap that exception in an
 * unchecked exception, hence stopping the stream. This exception can then be caught and unwrapped
 * within the block that initiated the stream.
 */
public class StreamException extends RuntimeException {

  /**
   * Creates a new StreamException with the specified cause.
   *
   * @param cause The throwable being wrapped.
   */
  public StreamException(Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new StreamException with the specified message.
   *
   * @param message The throwable being wrapped.
   */
  public StreamException(String message) {
    super(message);
  }
}
