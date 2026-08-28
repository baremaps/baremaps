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



import java.util.ArrayList;
import java.util.List;

/**
 * A step is a group of tasks executed sequentially in a workflow. A step may depend on other steps
 * through the needs attribute.
 */
public class Step {

  private String id;
  private List<String> needs = new ArrayList<>();
  private List<Task> tasks = new ArrayList<>();

  /**
   * Constructs a step.
   */
  public Step() {}

  /**
   * Constructs a step.
   *
   * @param id the id of the step
   * @param needs the ids of the steps that must be executed before this step
   * @param tasks the tasks of the step
   */
  public Step(String id, List<String> needs, List<Task> tasks) {
    this.id = id;
    this.needs = needs;
    this.tasks = tasks;
  }

  /**
   * Returns the id of the step.
   *
   * @return the id of the step
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the step.
   *
   * @param id the id of the step
   * @return the step
   */
  public Step setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Returns the ids of the steps that must be executed before this step.
   *
   * @return the ids of the steps that must be executed before this step
   */
  public List<String> getNeeds() {
    return needs;
  }

  /**
   * Sets the ids of the steps that must be executed before this step.
   *
   * @param needs the ids of the steps that must be executed before this step
   * @return the step
   */
  public Step setNeeds(List<String> needs) {
    this.needs = needs;
    return this;
  }

  /**
   * Returns the tasks of the step.
   *
   * @return the tasks of the step
   */
  public List<Task> getTasks() {
    return tasks;
  }

  /**
   * Sets the tasks of the step.
   *
   * @param tasks the tasks of the step
   * @return the step
   */
  public Step setTasks(List<Task> tasks) {
    this.tasks = tasks;
    return this;
  }
}
