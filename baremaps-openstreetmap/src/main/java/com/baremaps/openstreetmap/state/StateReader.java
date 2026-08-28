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


import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.baremaps.openstreetmap.OpenStreetMapFormat.Reader;
import com.baremaps.openstreetmap.model.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for reading OSM state files. This code has been adapted from pyosmium (BSD 2-Clause
 * "Simplified" License).
 */
public class StateReader implements Reader<State> {

  private static final Logger logger = LoggerFactory.getLogger(StateReader.class);

  private final String replicationUrl;

  private final boolean balancedSearch;

  private final int retries;

  /**
   * Constructs a {@code StateReader}.
   */
  public StateReader() {
    this("https://planet.osm.org/replication/hour", true, 2);
  }

  /**
   * Constructs a {@code StateReader}.
   *
   * @param replicationUrl the replication URL
   * @param balancedSearch whether to use a balanced search
   */
  public StateReader(String replicationUrl, boolean balancedSearch) {
    this(replicationUrl, balancedSearch, 2);
  }

  /**
   * Constructs a {@code StateReader}.
   *
   * @param replicationUrl the replication URL
   * @param balancedSearch whether to use a balanced search
   * @param retries the number of retries
   */
  public StateReader(String replicationUrl, boolean balancedSearch, int retries) {
    this.replicationUrl = replicationUrl;
    this.balancedSearch = balancedSearch;
    this.retries = retries;
  }

  /**
   * Parse an OSM state file.
   *
   * @param input the OpenStreetMap state file
   * @return the state
   */
  @Override
  public State read(InputStream input) {
    try {
      InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
      Map<String, String> map = new HashMap<>();
      for (String line : CharStreams.readLines(reader)) {
        String[] array = line.split("=");
        if (array.length == 2) {
          map.put(array[0], array[1]);
        }
      }
      long sequenceNumber = Long.parseLong(map.get("sequenceNumber"));
      DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
      LocalDateTime timestamp = LocalDateTime.parse(map.get("timestamp").replace("\\", ""), format);
      return new State(sequenceNumber, timestamp);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the state corresponding to the given timestamp.
   *
   * @param timestamp the timestamp
   * @return the state
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
    var lower = Optional.<State>empty();
    var lowerId = 0L;
    while (lower.isEmpty()) {
      lower = getLatestState(lowerId);
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
    long baseSplitId;
    while (true) {
      if (balancedSearch) {
        baseSplitId = ((lower.get().sequenceNumber() + upper.get().sequenceNumber()) / 2);
      } else {
        var tsInt = upper.get().timestamp().toEpochSecond(ZoneOffset.UTC)
            - lower.get().timestamp().toEpochSecond(ZoneOffset.UTC);
        var seqInt = upper.get().sequenceNumber() - lower.get().sequenceNumber();
        var goal = timestamp.getSecond() - lower.get().timestamp().getSecond();
        baseSplitId =
            lower.get().sequenceNumber() + (long) Math.ceil((double) (goal * seqInt) / tsInt);
        if (baseSplitId >= upper.get().sequenceNumber()) {
          baseSplitId = upper.get().sequenceNumber() - 1;
        }
      }
      var split = getLatestState(baseSplitId);
      if (split.isEmpty()) {
        var splitId = baseSplitId - 1;
        while (split.isEmpty() && splitId > lower.get().sequenceNumber()) {
          split = getLatestState(splitId);
          splitId--;
        }
      }
      if (split.isEmpty()) {
        var splitId = baseSplitId + 1;
        while (split.isEmpty() && splitId < upper.get().sequenceNumber()) {
          split = getLatestState(splitId);
          splitId++;
        }
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
   * Get the state corresponding to the given sequence number.
   *
   * @param sequenceNumber the sequence number
   * @return the state
   */
  public Optional<State> getLatestState(long sequenceNumber) {
    for (int i = 0; i < retries + 1; i++) {
      try (var inputStream = getStateUrl(sequenceNumber).openStream()) {
        var state = new StateReader().read(inputStream);
        return Optional.of(state);
      } catch (Exception e) {
        logger.error("Error while reading state file", e);
      }
    }
    return Optional.empty();
  }

  /**
   * Get the latest state.
   *
   * @return the state
   */
  public Optional<State> getLatestState() {
    try (var inputStream = getStateUrl().openStream()) {
      var state = new StateReader().read(inputStream);
      return Optional.of(state);
    } catch (Exception e) {
      logger.error("Error while reading state file", e);
    }
    return Optional.empty();
  }

  /**
   * Get the URL of the state file corresponding to the given sequence number.
   *
   * @param sequenceNumber the sequence number
   * @return the URL
   * @throws MalformedURLException if the URL is malformed
   */
  public URL getStateUrl(long sequenceNumber) throws MalformedURLException {
    var s = String.format("%09d", sequenceNumber);
    var uri =
        String.format("%s/%s/%s/%s.%s", replicationUrl, s.substring(0, 3), s.substring(3, 6),
            s.substring(6, 9), "state.txt");
    return URI.create(uri).toURL();
  }

  /**
   * Get the URL of the latest state file.
   *
   * @return the URL
   * @throws MalformedURLException if the URL is malformed
   */
  public URL getStateUrl() throws MalformedURLException {
    return new URL(replicationUrl + "/state.txt");
  }

  /**
   * Get the URL of a replication file.
   *
   * @param replicationUrl the replication URL
   * @param sequenceNumber the sequence number
   * @param extension the extension
   * @return the URL
   * @throws MalformedURLException if the URL is malformed
   */
  public URL getUrl(String replicationUrl, Long sequenceNumber, String extension)
      throws MalformedURLException {
    var s = String.format("%09d", sequenceNumber);
    var uri = String.format("%s/%s/%s/%s.%s", replicationUrl, s.substring(0, 3), s.substring(3, 6),
        s.substring(6, 9), extension);
    return URI.create(uri).toURL();
  }

}
