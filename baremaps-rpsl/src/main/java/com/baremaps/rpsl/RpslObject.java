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

package com.baremaps.rpsl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import net.ripe.ipresource.IpRange;

/**
 * Represents a RPSL Object.
 */
public record RpslObject(List<RpslAttribute> attributes) {

  /**
   * Constructs a RPSL Object.
   *
   * @param attributes a list of RPSL attributes
   */
  public RpslObject {
    checkNotNull(attributes);
    checkArgument(!attributes.isEmpty());
  }

  /**
   * Returns the type of the RPSL object.
   *
   * @return the type of the RPSL object
   */
  public String type() {
    return attributes.get(0).name();
  }

  /**
   * Returns the id of the RPSL object.
   *
   * @return the id of the RPSL object
   */
  public String id() {
    return attributes.get(0).value();
  }

  /**
   * Returns the first attribute value matching the attribute name.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  public Optional<String> first(String name) {
    return attributes.stream()
        .filter(attribute -> attribute.name().equals(name))
        .map(RpslAttribute::value)
        .findFirst();
  }

  /**
   * Returns all the attribute values matching the attribute name.
   *
   * @param name the attribute name
   * @return the attribute values
   */
  public List<String> all(String name) {
    return attributes.stream()
        .filter(attribute -> attribute.name().equals(name))
        .map(RpslAttribute::value)
        .toList();
  }

  /**
   * Return the attributes as a map.
   *
   * @return the attributes as a map
   */
  public Map<String, List<String>> asMap() {
    var map = new HashMap<String, List<String>>();
    for (RpslAttribute attribute : attributes()) {
      var list = map.getOrDefault(attribute.name(), new ArrayList<>());
      list.add(attribute.value());
      map.put(attribute.name(), list);
    }
    return map;
  }

  /**
   * Parses the 'inetnum' attribute into an IpRange.
   *
   * @return an Optional containing the IpRange
   */
  public Optional<IpRange> inetnum() {
    return first("inetnum").map(IpRange::parse);
  }

  /**
   * Parses the 'inet6num' attribute into an IpRange.
   *
   * @return an Optional containing the IpRange
   */
  public Optional<IpRange> inet6num() {
    return first("inet6num").map(IpRange::parse);
  }

  /**
   * Parses the 'changed' attributes into a list of Changed objects.
   *
   * @return a list of Changed objects
   */
  public List<RpslChanged> changed() {
    return all("changed").stream()
        .map(RpslChanged::parse)
        .collect(Collectors.toList());
  }

  /**
   * Parses the 'created' attribute into a LocalDateTime.
   *
   * @return an Optional containing the creation date
   */
  public Optional<LocalDateTime> created() {
    return first("created").map(this::parseDateTime);
  }

  /**
   * Parses the 'last-modified' attribute into a LocalDateTime.
   *
   * @return an Optional containing the last modification date
   */
  public Optional<LocalDateTime> lastModified() {
    return first("last-modified").map(this::parseDateTime);
  }

  private LocalDateTime parseDateTime(String dateTimeStr) {
    try {
      return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date time format: " + dateTimeStr, e);
    }
  }

  /**
   * Returns the 'status' attribute.
   *
   * @return an Optional containing the status
   */
  public Optional<String> status() {
    return first("status");
  }

  /**
   * Returns the 'netname' attribute.
   *
   * @return an Optional containing the netname
   */
  public Optional<String> netname() {
    return first("netname");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    var stringBuilder = new StringBuilder();
    for (RpslAttribute attribute : attributes()) {
      stringBuilder.append(attribute.name()).append(": ").append(attribute.value()).append("\n");
    }
    return stringBuilder.toString();
  }


  /**
   * Represents a RPSL attribute.
   */
  public record RpslAttribute(String name, String value) {

  }

  /**
   * Represents a changed attribute containing an email and a date.
   */
  public record RpslChanged(String email, LocalDate date) {

    /**
     * Parses a 'changed' attribute string into a Changed object.
     *
     * @param s the string to parse
     * @return the Changed object
     */
    public static RpslChanged parse(String s) {
      String[] parts = s.trim().split("\\s+");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid changed format: " + s);
      }
      String email = parts[0];
      String dateStr = parts[1];
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      LocalDate date;
      try {
        date = LocalDate.parse(dateStr, formatter);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Invalid date format in changed: " + dateStr, e);
      }
      return new RpslChanged(email, date);
    }
  }
}
