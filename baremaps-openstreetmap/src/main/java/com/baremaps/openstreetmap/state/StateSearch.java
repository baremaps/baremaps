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

package com.baremaps.openstreetmap.state;

import com.baremaps.openstreetmap.model.State;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Locates the state of a replication server that precedes a timestamp.
 *
 * <p>
 * A replication server publishes one state per sequence number and offers no index from timestamps
 * back to them, so the sequence has to be searched. States are ordered by timestamp, which makes
 * the search a bisection; the complication is that a sequence number may have no state at all,
 * because the server prunes the old ones. The search is expressed against a probe so that it can be
 * exercised without a server. Adapted from pyosmium (BSD 2-Clause "Simplified" License).
 */
class StateSearch {

  private final LongFunction<Optional<State>> probe;

  /**
   * Constructs a search over the states a probe can reach.
   *
   * @param probe returns the state with a sequence number, or empty if it cannot be read
   */
  StateSearch(LongFunction<Optional<State>> probe) {
    this.probe = probe;
  }

  /**
   * Finds the latest state whose timestamp precedes the given one.
   *
   * @param timestamp the timestamp
   * @param latest the most recent state of the server
   * @return the state
   */
  State before(LocalDateTime timestamp, State latest) {
    if (timestamp.isAfter(latest.timestamp()) || latest.sequenceNumber() <= 0) {
      return latest;
    }
    return switch (bracket(timestamp, latest)) {
      case Bounds bounds -> bisect(timestamp, bounds);
      case Answer answer -> answer.state();
    };
  }

  /**
   * Looks for a state older than the timestamp, halving the sequence number until one turns up.
   *
   * <p>
   * The search cannot simply walk up from the first sequence number, because the server may have
   * pruned it and every missing state costs a request. Halving reaches a surviving state in a
   * logarithmic number of probes. A candidate that turns out to be more recent than the timestamp
   * becomes the new upper bound and the halving starts over below it.
   */
  private Bracket bracket(LocalDateTime timestamp, State latest) {
    var upper = latest;
    var lowerId = 0L;
    while (true) {
      var lower = probe.apply(lowerId);
      if (lower.isPresent() && lower.get().timestamp().isAfter(timestamp)) {
        if (lower.get().sequenceNumber() == 0
            || lower.get().sequenceNumber() + 1 >= upper.sequenceNumber()) {
          // There is no room left below the candidate to look for an older state.
          return new Answer(lower.get());
        }
        upper = lower.get();
        lowerId = 0L;
        lower = Optional.empty();
      }
      if (lower.isPresent()) {
        return new Bounds(lower.get(), upper);
      }
      var nextId = (lowerId + upper.sequenceNumber()) / 2;
      if (nextId <= lowerId) {
        // Halving no longer makes progress: every state below the upper bound is unreachable.
        return new Answer(upper);
      }
      lowerId = nextId;
    }
  }

  /** Narrows the bounds by halving until they are adjacent, and returns the older one. */
  private State bisect(LocalDateTime timestamp, Bounds bounds) {
    var lower = bounds.lower();
    var upper = bounds.upper();
    while (lower.sequenceNumber() + 1 < upper.sequenceNumber()) {
      var split = probeNear((lower.sequenceNumber() + upper.sequenceNumber()) / 2, lower, upper);
      if (split.isEmpty()) {
        return lower;
      }
      if (split.get().timestamp().isBefore(timestamp)) {
        lower = split.get();
      } else {
        upper = split.get();
      }
    }
    return lower;
  }

  /**
   * Returns the state at the sequence number, or the closest one strictly between the bounds. The
   * midpoint of a bisection is often a pruned state, so its neighbours are tried before the search
   * gives up.
   */
  private Optional<State> probeNear(long sequenceNumber, State lower, State upper) {
    var state = probe.apply(sequenceNumber);
    for (var id = sequenceNumber - 1; state.isEmpty() && id > lower.sequenceNumber(); id--) {
      state = probe.apply(id);
    }
    for (var id = sequenceNumber + 1; state.isEmpty() && id < upper.sequenceNumber(); id++) {
      state = probe.apply(id);
    }
    return state;
  }

  /** The outcome of bracketing: either two bounds to bisect, or the answer itself. */
  private sealed interface Bracket {
  }

  /** A state older than the timestamp and one more recent than it. */
  private record Bounds(State lower, State upper) implements Bracket {
  }

  /** The state the search settled on without needing to bisect. */
  private record Answer(State state) implements Bracket {
  }
}
