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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.openstreetmap.model.State;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongPredicate;
import org.junit.jupiter.api.Test;

class StateSearchTest {

  private static final LocalDateTime EPOCH =
      LocalDateTime.of(2020, Month.JANUARY, 1, 0, 0);

  /** A server publishing one state per hour, of which only some sequence numbers survive. */
  private static class Server {

    private final long latestId;
    private final LongPredicate published;
    private final Set<Long> probed = new HashSet<>();

    Server(long latestId, LongPredicate published) {
      this.latestId = latestId;
      this.published = published;
    }

    Optional<State> probe(long sequenceNumber) {
      probed.add(sequenceNumber);
      if (sequenceNumber < 0 || sequenceNumber > latestId || !published.test(sequenceNumber)) {
        return Optional.empty();
      }
      return Optional.of(state(sequenceNumber));
    }

    State latest() {
      return state(latestId);
    }
  }

  private static State state(long sequenceNumber) {
    return new State(sequenceNumber, EPOCH.plusHours(sequenceNumber));
  }

  private static LocalDateTime timestampOf(long sequenceNumber) {
    return EPOCH.plusHours(sequenceNumber);
  }

  @Test
  void returnsTheLatestStateWhenTheTimestampIsInTheFuture() {
    var server = new Server(1000, id -> true);
    var found = new StateSearch(server::probe).before(timestampOf(5000), server.latest());
    assertEquals(1000, found.sequenceNumber());
  }

  @Test
  void returnsTheLatestStateWhenTheServerHasASingleState() {
    var server = new Server(0, id -> true);
    var found = new StateSearch(server::probe).before(timestampOf(-10), server.latest());
    assertEquals(0, found.sequenceNumber());
  }

  @Test
  void findsTheStateBeforeATimestampOnACompleteSequence() {
    var server = new Server(1000, id -> true);
    var search = new StateSearch(server::probe);
    for (long target : new long[] {1, 2, 137, 500, 999}) {
      // A timestamp halfway through an hour must resolve to the state that opened it.
      var found = search.before(timestampOf(target).plusMinutes(30), server.latest());
      assertEquals(target, found.sequenceNumber(), "target " + target);
    }
  }

  @Test
  void findsTheStateBeforeATimestampWhenStatesArePruned() {
    // Only every seventh state survives, so most probes come back empty.
    var server = new Server(1000, id -> id % 7 == 0);
    var found = new StateSearch(server::probe).before(timestampOf(500), server.latest());
    assertTrue(found.timestamp().isBefore(timestampOf(500)));
    // The answer is the newest surviving state before the timestamp, 497 = 71 * 7.
    assertEquals(497, found.sequenceNumber());
  }

  @Test
  void searchesALargeSequenceInAFewProbes() {
    var server = new Server(1_000_000, id -> true);
    new StateSearch(server::probe).before(timestampOf(654_321), server.latest());
    // A linear walk would need hundreds of thousands of requests; bisection needs about 2 log2(n).
    assertTrue(server.probed.size() < 100, "probed " + server.probed.size() + " sequence numbers");
  }

  @Test
  void returnsTheOldestReachableStateWhenEveryStateIsTooRecent() {
    var server = new Server(1000, id -> true);
    var found = new StateSearch(server::probe).before(timestampOf(-1), server.latest());
    assertEquals(0, found.sequenceNumber());
  }

  @Test
  void returnsTheLatestStateWhenNoStateCanBeRead() {
    var server = new Server(1000, id -> false);
    var found = new StateSearch(server::probe).before(timestampOf(500), server.latest());
    assertEquals(1000, found.sequenceNumber());
  }
}
