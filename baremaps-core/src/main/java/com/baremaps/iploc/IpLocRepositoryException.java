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

package com.baremaps.iploc;

/**
 * Signals that an operation on an {@link IpLocRepository} failed.
 *
 * <p>
 * Unchecked so that the repository can be used from the lambdas of a stream, which is how an index
 * is built.
 */
public class IpLocRepositoryException extends RuntimeException {

  /**
   * Constructs an {@code IpLocRepositoryException} with the specified detail message and cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public IpLocRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
