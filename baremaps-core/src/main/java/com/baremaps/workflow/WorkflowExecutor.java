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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
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
    this.graph = dependencyGraph(this.steps.values());
  }

  /**
   * Returns the graph whose edges point from a step to the steps that need it, so that a step can
   * be scheduled as soon as its predecessors complete.
   *
   * @param steps the steps of the workflow
   * @return the dependency graph
   * @throws WorkflowException if the dependencies form a cycle
   */
  private static Graph<String> dependencyGraph(Collection<Step> steps) {
    ImmutableGraph.Builder<String> builder = GraphBuilder.directed().immutable();
    for (Step step : steps) {
      builder.addNode(step.getId());
    }
    for (Step step : steps) {
      if (step.getNeeds() != null) {
        for (String stepNeeded : step.getNeeds()) {
          builder.putEdge(stepNeeded, step.getId());
        }
      }
    }
    var graph = builder.build();
    if (Graphs.hasCycle(graph)) {
      throw new WorkflowException("The workflow must be a directed acyclic graph");
    }
    return graph;
  }

  /**
   * Executes the workflow.
   */
  public void execute() {
    try {
      executeAsync().join();
    } finally {
      // The measures of the tasks that did run are worth reporting even when a later task failed.
      logMeasures();
    }
  }

  public CompletableFuture<Void> executeAsync() {
    // Create futures for each end step
    var endSteps = graph.nodes().stream()
        .filter(this::isEndStep)
        .map(this::stepFuture)
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
  private CompletableFuture<Void> stepFuture(String step) {
    return futures.computeIfAbsent(step, this::createStepFuture);
  }

  /**
   * Creates a future step associated to the step id.
   *
   * @param stepId the step id
   * @return the future step
   */
  private CompletableFuture<Void> createStepFuture(String stepId) {
    // Initialize the future step with the previous future step
    // as it depends on its completion.
    var future = predecessorsFuture(stepId);

    // Time the execution of the tasks. The list is appended to by the pool threads running the
    // tasks and read by logMeasures, which runs while sibling steps may still be in flight.
    var measures = new CopyOnWriteArrayList<TaskMeasure>();

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

    // The measures are registered now and filled in as the tasks run.
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
  private CompletableFuture<Void> predecessorsFuture(String stepId) {
    var predecessors = graph.predecessors(stepId).stream().toList();

    // If the step has no predecessor,
    // return an empty completed future step.
    if (predecessors.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    // If the step has one predecessor,
    // return the future step associated to it.
    if (predecessors.size() == 1) {
      return stepFuture(predecessors.get(0));
    }

    // If the step has multiple predecessors,
    // return a future step that completes when all the predecessors complete.
    var futurePredecessors = predecessors.stream()
        .map(this::stepFuture)
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futurePredecessors);
  }

  /** Logs how long the workflow, each of its steps, and each of their tasks took. */
  private void logMeasures() {
    logger.info("----------------------------------------");
    logger.info("  Duration: {}",
        format(span(stepMeasures.stream().flatMap(step -> step.taskMeasures().stream()).toList())));
    for (var stepMeasure : stepMeasures) {
      logger.info("Step: {}, Duration: {}", stepMeasure.step().getId(),
          format(span(stepMeasure.taskMeasures())));
      for (var taskMeasure : stepMeasure.taskMeasures()) {
        logger.info("  Task: {}", taskMeasure.task());
        logger.info("    Duration: {}", format(span(List.of(taskMeasure))));
      }
    }
    logger.info("----------------------------------------");
  }

  /**
   * Returns the wall clock time the measures span, or empty when there is none to report. Steps run
   * in parallel, so a group takes as long as its extent, not as long as the sum of its parts.
   */
  private static Optional<Duration> span(List<TaskMeasure> measures) {
    if (measures.isEmpty()) {
      return Optional.empty();
    }
    var start = measures.stream().mapToLong(TaskMeasure::start).min().getAsLong();
    var end = measures.stream().mapToLong(TaskMeasure::end).max().getAsLong();
    return Optional.of(Duration.ofMillis(end - start));
  }

  /** Formats a duration in its non-zero units only, e.g. {@code "1 hrs 3 s"}. */
  private static String format(Optional<Duration> duration) {
    if (duration.isEmpty()) {
      return "unknown";
    }
    var value = duration.get();
    if (value.isZero()) {
      return "0 ms";
    }
    var parts = new long[] {value.toDaysPart(), value.toHoursPart(), value.toMinutesPart(),
        value.toSecondsPart(), value.toMillisPart()};
    var units = new String[] {"days", "hrs", "min", "s", "ms"};
    var formatted = new StringJoiner(" ");
    for (int i = 0; i < parts.length; i++) {
      if (parts[i] > 0) {
        formatted.add(parts[i] + " " + units[i]);
      }
    }
    return formatted.toString();
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

  record StepMeasure(Step step, List<TaskMeasure> taskMeasures) {
  }

  record TaskMeasure(Task task, long start, long end) {
  }

}
