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
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the state files of an OpenStreetMap replication server and locates the state matching a
 * timestamp. Adapted from pyosmium (BSD 2-Clause "Simplified" License).
 */
public class StateReader {

  private static final Logger logger = LoggerFactory.getLogger(StateReader.class);

  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private static final String DEFAULT_REPLICATION_URL = "https://planet.osm.org/replication/hour";

  private static final int DEFAULT_RETRIES = 2;

  private final String replicationUrl;
  private final int retries;

  public StateReader() {
    this(DEFAULT_REPLICATION_URL);
  }

  public StateReader(String replicationUrl) {
    this(replicationUrl, DEFAULT_RETRIES);
  }

  /**
   * @param replicationUrl the base URL of the replication server
   * @param retries how many times a failed download of a state file is retried
   */
  public StateReader(String replicationUrl, int retries) {
    this.replicationUrl = replicationUrl;
    this.retries = retries;
  }

  /**
   * Parses a state file, a list of {@code key=value} lines.
   *
   * @param input the state file
   * @return the state
   */
  public State read(InputStream input) {
    try {
      Map<String, String> map = new HashMap<>();
      for (String line : CharStreams
          .readLines(new InputStreamReader(input, StandardCharsets.UTF_8))) {
        String[] array = line.split("=");
        if (array.length == 2) {
          map.put(array[0], array[1]);
        }
      }
      long sequenceNumber = Long.parseLong(map.get("sequenceNumber"));
      // The colons of the timestamp are escaped with backslashes in state files.
      String timestamp = map.get("timestamp").replace("\\", "");
      return new State(sequenceNumber, LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Finds the latest state whose timestamp precedes the given one.
   *
   * <p>
   * The search first brackets the timestamp: starting from the latest state as the upper bound, it
   * halves the sequence number until it finds a state before the timestamp (the lower bound).
   * Then it bisects between the bounds. State files can be missing on the server, so a probe that
   * fails is retried on neighbouring sequence numbers.
   *
   * @param timestamp the timestamp
   * @return the state, or empty if the latest state cannot be read
   */
  @SuppressWarnings({"squid:S3776", "squid:S6541"})
  public Optional<State> getStateFromTimestamp(LocalDateTime timestamp) {
    var upper = getLatestState();
    if (upper.isEmpty()) {
      return Optional.empty();
    }
    if (timestamp.isAfter(upper.get().timestamp()) || upper.get().sequenceNumber() <= 0) {
      return upper;
    }

    // Bracket the timestamp between a lower and an upper state.
    var lower = Optional.<State>empty();
    var lowerId = 0L;
    while (lower.isEmpty()) {
      lower = getState(lowerId);
      if (lower.isPresent() && lower.get().timestamp().isAfter(timestamp)) {
        if (lower.get().sequenceNumber() == 0
            || lower.get().sequenceNumber() + 1 >= upper.get().sequenceNumber()) {
          return lower;
        }
        upper = lower;
        lower = Optional.empty();
        lowerId = 0L;
      }
      if (lower.isEmpty()) {
        var newId = (lowerId + upper.get().sequenceNumber()) / 2;
        if (newId <= lowerId) {
          return upper;
        }
        lowerId = newId;
      }
    }

    // Bisect between the bounds.
    while (true) {
      var splitId = (lower.get().sequenceNumber() + upper.get().sequenceNumber()) / 2;
      var split = getState(splitId);
      for (var id = splitId - 1; split.isEmpty() && id > lower.get().sequenceNumber(); id--) {
        split = getState(id);
      }
      for (var id = splitId + 1; split.isEmpty() && id < upper.get().sequenceNumber(); id++) {
        split = getState(id);
      }
      if (split.isEmpty()) {
        return lower;
      }
      if (split.get().timestamp().isBefore(timestamp)) {
        lower = split;
      } else {
        upper = split;
      }
      if (lower.get().sequenceNumber() + 1 >= upper.get().sequenceNumber()) {
        return lower;
      }
    }
  }

  /**
   * Downloads the state with the given sequence number.
   *
   * @param sequenceNumber the sequence number
   * @return the state, or empty if it cannot be read
   */
  public Optional<State> getState(long sequenceNumber) {
    for (int i = 0; i <= retries; i++) {
      try (var input = getUrl(sequenceNumber, "state.txt").openStream()) {
        return Optional.of(read(input));
      } catch (Exception e) {
        logger.error("Error while reading state file", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Downloads the latest state of the replication server.
   *
   * @return the state, or empty if it cannot be read
   */
  public Optional<State> getLatestState() {
    try (var input = URI.create(replicationUrl + "/state.txt").toURL().openStream()) {
      return Optional.of(read(input));
    } catch (Exception e) {
      logger.error("Error while reading state file", e);
      return Optional.empty();
    }
  }

  /**
   * Returns the URL of a replication file. Sequence numbers are zero-padded to nine digits and
   * split in three directory levels, e.g. 1234 becomes 000/001/234.
   *
   * @param sequenceNumber the sequence number
   * @param extension the file extension, e.g. "state.txt" or "osc.gz"
   * @return the URL
   * @throws MalformedURLException if the URL is malformed
   */
  public URL getUrl(long sequenceNumber, String extension) throws MalformedURLException {
    var s = String.format("%09d", sequenceNumber);
    return URI.create(String.format("%s/%s/%s/%s.%s", replicationUrl, s.substring(0, 3),
        s.substring(3, 6), s.substring(6, 9), extension)).toURL();
  }
}
