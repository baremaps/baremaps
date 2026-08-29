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

package com.baremaps.openstreetmap.xml;

import static javax.xml.stream.XMLInputFactory.IS_COALESCING;
import static javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import com.baremaps.data.stream.StreamException;
import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Decodes the elements shared by the OSM XML formats (osm and osc) from a StAX cursor.
 *
 * <p>
 * Every {@code readX} method expects the cursor on the START_ELEMENT of the element and leaves it
 * on the matching END_ELEMENT, so callers can chain reads with {@link XMLStreamReader#nextTag()}.
 *
 * <p>
 * Attributes are parsed leniently: change files omit the coordinates of deleted nodes and some
 * exporters drop the editing metadata.
 */
final class XmlElementReader {

  static final String NODE = "node";
  static final String WAY = "way";
  static final String RELATION = "relation";

  static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final XMLStreamReader reader;

  XmlElementReader(InputStream input) {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(SUPPORT_DTD, false);
    factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(IS_NAMESPACE_AWARE, false);
    factory.setProperty(IS_VALIDATING, false);
    factory.setProperty(IS_COALESCING, false);
    try {
      this.reader = factory.createXMLStreamReader(input);
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  XMLStreamReader cursor() {
    return reader;
  }

  /** Reads the node, way or relation the cursor is on. */
  Element readElement() throws XMLStreamException {
    return switch (reader.getLocalName()) {
      case NODE -> readNode();
      case WAY -> readWay();
      case RELATION -> readRelation();
      default -> throw new StreamException("Unexpected XML element: " + reader.getLocalName());
    };
  }

  Header readHeader() {
    String generator = attribute("generator");
    String source = attribute("source");
    String replicationUrl = attribute("osmosis_replication_url");
    Long replicationSequenceNumber = parseLong(attribute("osmosis_replication_sequence_number"));
    // Prefer the replication timestamp: the plain timestamp is the export time, which can be later.
    LocalDateTime timestamp = parseTimestamp(attribute("osmosis_replication_timestamp"));
    if (timestamp == null) {
      timestamp = parseTimestamp(attribute("timestamp"));
    }
    return new Header(replicationSequenceNumber, timestamp, replicationUrl, source, generator);
  }

  Bound readBounds() {
    return new Bound(
        Double.parseDouble(attribute("maxlat")),
        Double.parseDouble(attribute("maxlon")),
        Double.parseDouble(attribute("minlat")),
        Double.parseDouble(attribute("minlon")));
  }

  Node readNode() throws XMLStreamException {
    long id = Long.parseLong(attribute("id"));
    Info info = readInfo();
    double lat = parseDouble(attribute("lat"));
    double lon = parseDouble(attribute("lon"));
    Map<String, Object> tags = new HashMap<>();
    reader.nextTag();
    while (reader.getEventType() == START_ELEMENT) {
      if ("tag".equals(reader.getLocalName())) {
        readTag(tags);
      } else {
        skipElement();
      }
      reader.nextTag();
    }
    return new Node(id, info, tags, lon, lat);
  }

  Way readWay() throws XMLStreamException {
    long id = Long.parseLong(attribute("id"));
    Info info = readInfo();
    Map<String, Object> tags = new HashMap<>();
    List<Long> nodes = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == START_ELEMENT) {
      switch (reader.getLocalName()) {
        case "tag" -> readTag(tags);
        case "nd" -> {
          nodes.add(Long.parseLong(attribute("ref")));
          reader.nextTag();
        }
        default -> skipElement();
      }
      reader.nextTag();
    }
    return new Way(id, info, tags, nodes);
  }

  Relation readRelation() throws XMLStreamException {
    long id = Long.parseLong(attribute("id"));
    Info info = readInfo();
    Map<String, Object> tags = new HashMap<>();
    List<Member> members = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == START_ELEMENT) {
      switch (reader.getLocalName()) {
        case "tag" -> readTag(tags);
        case "member" -> {
          long ref = Long.parseLong(attribute("ref"));
          Member.MemberType type = Member.MemberType.valueOf(attribute("type").toUpperCase());
          members.add(new Member(ref, type, attribute("role")));
          reader.nextTag();
        }
        default -> skipElement();
      }
      reader.nextTag();
    }
    return new Relation(id, info, tags, members);
  }

  private Info readInfo() {
    Long version = parseLong(attribute("version"));
    Long changeset = parseLong(attribute("changeset"));
    Long uid = parseLong(attribute("uid"));
    return new Info(
        version != null ? version.intValue() : Info.NO_INFO.version(),
        parseTimestamp(attribute("timestamp")),
        changeset != null ? changeset : Info.NO_INFO.changeset(),
        uid != null ? uid.intValue() : Info.NO_INFO.uid());
  }

  private void readTag(Map<String, Object> tags) throws XMLStreamException {
    tags.put(attribute("k"), attribute("v"));
    reader.nextTag();
  }

  /** Skips the element the cursor is on, leaving the cursor on its END_ELEMENT. */
  void skipElement() throws XMLStreamException {
    int depth = 1;
    while (depth > 0) {
      int event = reader.next();
      if (event == START_ELEMENT) {
        depth++;
      } else if (event == END_ELEMENT) {
        depth--;
      }
    }
  }

  private String attribute(String name) {
    return reader.getAttributeValue(null, name);
  }

  private static Long parseLong(String value) {
    return value != null ? Long.valueOf(value) : null;
  }

  private static double parseDouble(String value) {
    return value != null ? Double.parseDouble(value) : Double.NaN;
  }

  private static LocalDateTime parseTimestamp(String value) {
    return value != null ? LocalDateTime.parse(value, TIMESTAMP_FORMAT) : null;
  }
}
