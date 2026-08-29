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
import java.io.BufferedReader;
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

/** Reads the state files of an OpenStreetMap replication server. */
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
      Map<String, String> values = new HashMap<>();
      var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
      for (String line = reader.readLine(); line != null; line = reader.readLine()) {
        // The value of a timestamp holds colons, so only the first separator delimits the key.
        int separator = line.indexOf('=');
        if (separator > 0) {
          values.put(line.substring(0, separator), line.substring(separator + 1));
        }
      }
      long sequenceNumber = Long.parseLong(values.get("sequenceNumber"));
      // The colons of the timestamp are escaped with backslashes in state files.
      String timestamp = values.get("timestamp").replace("\\", "");
      return new State(sequenceNumber, LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Finds the latest state whose timestamp precedes the given one.
   *
   * @param timestamp the timestamp
   * @return the state, or empty if the latest state cannot be read
   */
  public Optional<State> getStateFromTimestamp(LocalDateTime timestamp) {
    return getLatestState()
        .map(latest -> new StateSearch(this::getState).before(timestamp, latest));
  }

  /**
   * Downloads the state with the given sequence number.
   *
   * <p>
   * A state that cannot be read is not an error: the server prunes old states, and locating one by
   * timestamp probes sequence numbers that are expected to be missing.
   *
   * @param sequenceNumber the sequence number
   * @return the state, or empty if it cannot be read
   */
  public Optional<State> getState(long sequenceNumber) {
    for (int i = 0; i <= retries; i++) {
      try (var input = getUrl(sequenceNumber, "state.txt").openStream()) {
        return Optional.of(read(input));
      } catch (Exception e) {
        logger.debug("Unable to read the state file #{}", sequenceNumber, e);
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
      logger.error("Unable to read the latest state file of {}", replicationUrl, e);
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
