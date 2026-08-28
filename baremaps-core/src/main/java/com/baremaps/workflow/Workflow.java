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
 * A workflow is a graph of steps that can be executed in parallel. Steps are nodes of a directed
 * acyclic graph (DAG) and each step may depend on other steps through the needs attribute. The
 * directed acyclic graph is built, validated and executed by the {@link WorkflowExecutor}.
 */
public class Workflow {

  private List<Step> steps = new ArrayList<>();

  /**
   * Constructs a workflow.
   */
  public Workflow() {}

  /**
   * Constructs a workflow.
   *
   * @param steps the steps of the workflow
   */
  public Workflow(List<Step> steps) {
    this.steps = steps;
  }

  /**
   * Returns the steps of the workflow.
   *
   * @return the steps of the workflow
   */
  public List<Step> getSteps() {
    return steps;
  }

  /**
   * Sets the steps of the workflow.
   *
   * @param steps the steps of the workflow
   * @return the workflow
   */
  public Workflow setSteps(List<Step> steps) {
    this.steps = steps;
    return this;
  }
}
