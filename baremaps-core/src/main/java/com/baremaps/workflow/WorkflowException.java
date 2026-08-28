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

package com.baremaps.workflow;

/** Signals that an exception occurred during the execution of a {@code Workflow}. */
public class WorkflowException extends RuntimeException {

  /** Constructs a {@code WorkflowException} with {@code null} as its error detail message. */
  public WorkflowException() {}

  /**
   * Constructs an {@code WorkflowException} with the specified detail message.
   *
   * @param message the message
   */
  public WorkflowException(String message) {
    super(message);
  }

  /**
   * Constructs a {@code WorkflowException} with the specified cause.
   *
   * @param cause the cause
   */
  public WorkflowException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a {@code WorkflowException} with the specified detail message and cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public WorkflowException(String message, Throwable cause) {
    super(message, cause);
  }
}
