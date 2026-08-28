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



import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A workflow executor executes a workflow in parallel.
 */
public class WorkflowExecutor implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);

  private final ExecutorService executorService;

  private final WorkflowContext context;

  private final Map<String, Step> steps;

  private final Map<String, CompletableFuture<Void>> futures;

  private final Graph<String> graph;

  private final List<StepMeasure> stepMeasures;

  /**
   * Constructs a workflow executor.
   *
   * @param workflow the workflow to execute
   */
  public WorkflowExecutor(Workflow workflow) {
    this(workflow, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
  }

  /**
   * Constructs a workflow executor.
   *
   * @param workflow the workflow to execute
   * @param executorService the executor service used to execute the tasks
   */
  public WorkflowExecutor(Workflow workflow, ExecutorService executorService) {
    this.executorService = executorService;
    this.context = new WorkflowContext();
    this.steps = workflow.getSteps().stream()
        .collect(Collectors.toMap(step -> step.getId(), step -> step));
    this.futures = new ConcurrentSkipListMap<>();
    this.stepMeasures = new CopyOnWriteArrayList<>();

    // Create a graph from the workflow
    ImmutableGraph.Builder<String> graphBuilder = GraphBuilder.directed().immutable();

    // Add the nodes (e.g. steps) to the graph
    for (String id : this.steps.keySet()) {
      graphBuilder.addNode(id);
    }

    // Add the edges (e.g. needs) to the graph
    for (Step step : this.steps.values()) {
      if (step.getNeeds() != null) {
        for (String stepNeeded : step.getNeeds()) {
          graphBuilder.putEdge(stepNeeded, step.getId());
        }
      }
    }

    // Build the graph
    this.graph = graphBuilder.build();

    // Check that the graph is acyclic
    if (Graphs.hasCycle(this.graph)) {
      throw new WorkflowException("The workflow must be a directed acyclic graph");
    }
  }

  /**
   * Executes the workflow.
   */
  public void execute() {
    try {
      executeAsync().join();
      logStepMeasures();
    } catch (Exception e) {
      logger.error("Error while executing the workflow", e);
    }
  }

  public CompletableFuture<Void> executeAsync() {
    // Create futures for each end step
    var endSteps = graph.nodes().stream()
        .filter(this::isEndStep)
        .map(this::getStep)
        .toArray(CompletableFuture[]::new);

    // Wait for all the end steps to complete
    return CompletableFuture.allOf(endSteps);
  }

  /**
   * Returns the future step associated to the step id. If the future step does not exist, it is
   * created.
   *
   * @param step the step id
   * @return the future step
   */
  private CompletableFuture<Void> getStep(String step) {
    return futures.computeIfAbsent(step, this::createStep);
  }

  /**
   * Creates a future step associated to the step id.
   *
   * @param stepId the step id
   * @return the future step
   */
  private CompletableFuture<Void> createStep(String stepId) {
    // Initialize the future step with the previous future step
    // as it depends on its completion.
    var future = getPreviousStep(stepId);

    // Time the execution of the tasks
    var measures = new ArrayList<TaskMeasure>();

    // Get the step from the workflow and skip it if it does not exist.
    // This allows to comment out steps in the workflow without breaking the execution.
    var step = steps.get(stepId);
    if (step == null) {
      logger.warn("Step {} does not exist and will be skipped", stepId);
      return future;
    }

    // Chain the tasks of the step to the future so that they are executed
    // sequentially when the previous future step completes.
    var tasks = step.getTasks();
    for (var task : tasks) {
      future = future.thenRunAsync(() -> {
        try {
          logger.info("Executing task {} of step {}", task, stepId);
          var start = System.currentTimeMillis();
          task.execute(context);
          var end = System.currentTimeMillis();
          var measure = new TaskMeasure(task, start, end);
          measures.add(measure);
        } catch (Exception e) {
          throw new WorkflowException(e);
        }
      }, executorService);
    }

    // Record the measure
    this.stepMeasures.add(new StepMeasure(step, measures));

    return future;
  }

  /**
   * Returns the future step associated to the previous step of the step id. If the future step does
   * not exist, it is created.
   *
   * @param stepId the step id
   * @return the future step
   */
  private CompletableFuture<Void> getPreviousStep(String stepId) {
    var predecessors = graph.predecessors(stepId).stream().toList();

    // If the step has no predecessor,
    // return an empty completed future step.
    if (predecessors.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    // If the step has one predecessor,
    // return the future step associated to it.
    if (predecessors.size() == 1) {
      return getStep(predecessors.get(0));
    }

    // If the step has multiple predecessors,
    // return a future step that completes when all the predecessors complete.
    var futurePredecessors = predecessors.stream()
        .map(this::getStep)
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futurePredecessors);
  }

  /**
   * Logs the step measures.
   */
  private void logStepMeasures() {
    logger.info("----------------------------------------");

    var workflowStart = stepMeasures.stream()
        .flatMapToLong(measures -> measures.stepMeasures.stream()
            .mapToLong(measure -> measure.start))
        .min();

    var workflowEnd = stepMeasures.stream()
        .flatMapToLong(measures -> measures.stepMeasures.stream()
            .mapToLong(measure -> measure.end))
        .max();

    if (workflowStart.isPresent() && workflowEnd.isPresent()) {
      var workflowDuration = Duration.ofMillis(workflowEnd.getAsLong() - workflowStart.getAsLong());
      logger.info("  Duration: {}", formatDuration(workflowDuration));
    } else {
      logger.info("  Duration: unknown");
    }

    for (var stepMeasure : this.stepMeasures) {
      var stepStart = stepMeasure.stepMeasures.stream()
          .mapToLong(measure -> measure.start).min();
      var stepEnd = stepMeasure.stepMeasures.stream()
          .mapToLong(measure -> measure.end).max();

      if (stepStart.isPresent() && stepEnd.isPresent()) {
        var stepDuration = Duration.ofMillis(stepEnd.getAsLong() - stepStart.getAsLong());
        logger.info("Step: {}, Duration: {} ms", stepMeasure.step.getId(),
            formatDuration(stepDuration));
      } else {
        logger.info("Step: {}, Duration: unknown", stepMeasure.step.getId());
      }

      for (var taskMeasure : stepMeasure.stepMeasures) {
        var taskDuration = Duration.ofMillis(taskMeasure.end - taskMeasure.start);
        logger.info("  Task: {}", taskMeasure.task);
        logger.info("    Duration: {}", formatDuration(taskDuration));
      }
    }
    logger.info("----------------------------------------");
  }

  /**
   * Returns a string representation of the duration.
   *
   * @param duration the duration
   * @return a string representation of the duration
   */
  private static String formatDuration(Duration duration) {
    var builder = new StringBuilder();
    var days = duration.toDays();
    if (duration.isZero()) {
      builder.append("0 ms");
    } else {
      if (days > 0) {
        builder.append(days).append(" days ");
      }
      final long hrs = duration.toHours() - Duration.ofDays(duration.toDays()).toHours();
      if (hrs > 0) {
        builder.append(hrs).append(" hrs ");
      }
      final long min = duration.toMinutes() - Duration.ofHours(duration.toHours()).toMinutes();
      if (min > 0) {
        builder.append(min).append(" min ");
      }
      final long sec = duration.toSeconds() - Duration.ofMinutes(duration.toMinutes()).toSeconds();
      if (sec > 0) {
        builder.append(sec).append(" s ");
      }
      final long ms = duration.toMillis() - Duration.ofSeconds(duration.toSeconds()).toMillis();
      if (ms > 0) {
        builder.append(ms).append(" ms ");
      }
    }
    return builder.toString();
  }

  /**
   * Returns true if the step is an end step.
   *
   * @param stepId the step id
   * @return true if the step is an end step
   */
  private boolean isEndStep(String stepId) {
    return graph.successors(stepId).isEmpty();
  }

  /**
   * Closes the workflow executor.
   */
  @Override
  public void close() throws Exception {
    executorService.shutdown();
  }

  record StepMeasure(Step step, List<TaskMeasure> stepMeasures) {
  }

  record TaskMeasure(Task task, long start, long end) {
  }

}
