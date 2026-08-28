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
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.stream.StreamException;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * An object for traversing an OpenStreetMap XML file describing entities (osm.xml) and creating a
 * stream.
 */
public class XmlEntitySpliterator implements Spliterator<Entity> {

  private static final String ELEMENT_NAME_OSM = "osm";
  private static final String ELEMENT_NAME_BOUND = "bound";
  private static final String ELEMENT_NAME_BOUNDS = "bounds";
  private static final String ELEMENT_NAME_NODE = "node";
  private static final String ELEMENT_NAME_WAY = "way";
  private static final String ELEMENT_NAME_RELATION = "relation";
  private static final String ELEMENT_NAME_TAG = "tag";
  private static final String ELEMENT_NAME_NODE_REFERENCE = "nd";
  private static final String ELEMENT_NAME_MEMBER = "member";
  private static final String ATTRIBUTE_NAME_ID = "id";
  private static final String ATTRIBUTE_NAME_VERSION = "version";
  private static final String ATTRIBUTE_NAME_GENERATOR = "generator";
  private static final String ATTRIBUTE_NAME_SOURCE = "source";
  private static final String ATTRIBUTE_NAME_TIMESTAMP = "timestamp";
  private static final String ATTRIBUTE_NAME_OSMOSIS_REPLICATION_URL = "osmosis_replication_url";
  private static final String ATTRIBUTE_NAME_OSMOSIS_REPLICATION_TIMESTAMP =
      "osmosis_replication_timestamp";
  private static final String ATTRIBUTE_NAME_OSMOSIS_REPLICATION_SEQUENCE_NUMBER =
      "osmosis_replication_sequence_number";
  private static final String ATTRIBUTE_NAME_USER_ID = "uid";
  private static final String ATTRIBUTE_NAME_CHANGESET_ID = "changeset";
  private static final String ATTRIBUTE_NAME_LATITUDE = "lat";
  private static final String ATTRIBUTE_NAME_LONGITUDE = "lon";
  private static final String ATTRIBUTE_NAME_KEY = "k";
  private static final String ATTRIBUTE_NAME_VALUE = "v";
  private static final String ATTRIBUTE_NAME_REF = "ref";
  private static final String ATTRIBUTE_NAME_TYPE = "type";
  private static final String ATTRIBUTE_NAME_ROLE = "role";
  private static final String ATTRIBUTE_NAME_MAXLON = "maxlon";
  private static final String ATTRIBUTE_NAME_MAXLAT = "maxlat";
  private static final String ATTRIBUTE_NAME_MINLON = "minlon";
  private static final String ATTRIBUTE_NAME_MINLAT = "minlat";
  public static final DateTimeFormatter format =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private final XMLStreamReader reader;

  public XmlEntitySpliterator(InputStream input) {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(SUPPORT_DTD, false);
    factory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(IS_NAMESPACE_AWARE, false);
    factory.setProperty(IS_VALIDATING, false);
    factory.setProperty(IS_COALESCING, false);
    try {
      reader = factory.createXMLStreamReader(input);
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  @Override
  public boolean tryAdvance(Consumer<? super Entity> consumer) {
    try {
      if (reader.hasNext()) {
        int event = reader.next();
        switch (event) {
          case START_ELEMENT:
            readEntity(consumer);
            return true;
          case END_DOCUMENT:
            return false;
          default:
            return true;
        }
      } else {
        return false;
      }
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  private void readEntity(Consumer<? super Entity> consumer) throws XMLStreamException {
    switch (reader.getLocalName()) {
      case ELEMENT_NAME_OSM:
        consumer.accept(readHeader());
        return;
      case ELEMENT_NAME_BOUND, ELEMENT_NAME_BOUNDS:
        consumer.accept(readBounds());
        return;
      case ELEMENT_NAME_NODE:
        consumer.accept(readNode());
        return;
      case ELEMENT_NAME_WAY:
        consumer.accept(readWay());
        return;
      case ELEMENT_NAME_RELATION:
        consumer.accept(readRelation());
        return;
      default:
        readUnknownElement();
    }
  }

  private Header readHeader() {
    String generator = reader.getAttributeValue(null, ATTRIBUTE_NAME_GENERATOR);
    String source = reader.getAttributeValue(null, ATTRIBUTE_NAME_SOURCE);
    String replicationUrl = reader.getAttributeValue(null, ATTRIBUTE_NAME_OSMOSIS_REPLICATION_URL);
    String replicationSequenceNumberValue =
        reader.getAttributeValue(null, ATTRIBUTE_NAME_OSMOSIS_REPLICATION_SEQUENCE_NUMBER);
    Long replicationSequenceNumber =
        replicationSequenceNumberValue != null ? Long.parseLong(replicationSequenceNumberValue)
            : null;
    String timestampValue = reader.getAttributeValue(null, ATTRIBUTE_NAME_TIMESTAMP);
    LocalDateTime timestamp =
        timestampValue != null ? LocalDateTime.parse(timestampValue, format) : null;
    String osmosisReplicationTimestampValue =
        reader.getAttributeValue(null, ATTRIBUTE_NAME_OSMOSIS_REPLICATION_TIMESTAMP);
    timestamp = osmosisReplicationTimestampValue != null
        ? LocalDateTime.parse(osmosisReplicationTimestampValue, format)
        : timestamp;
    return new Header(replicationSequenceNumber, timestamp, replicationUrl, source, generator);
  }

  private Bound readBounds() {
    double maxLon = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_MAXLON));
    double maxLat = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_MAXLAT));
    double minLon = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_MINLON));
    double minLat = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_MINLAT));
    return new Bound(maxLat, maxLon, minLat, minLon);
  }

  private Node readNode() throws XMLStreamException {
    long id = Long.parseLong(reader.getAttributeValue(null, ATTRIBUTE_NAME_ID));
    Info info = readInfo();
    double latitude = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_LATITUDE));
    double longitude = Double.parseDouble(reader.getAttributeValue(null, ATTRIBUTE_NAME_LONGITUDE));

    // read the content of the node
    Map<String, Object> tags = new HashMap<>();
    reader.nextTag();
    while (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
      if (ELEMENT_NAME_TAG.equals(reader.getLocalName())) {
        readTag(tags);
      } else {
        readUnknownElement();
      }
    }

    return new Node(id, info, tags, longitude, latitude);
  }

  private Way readWay() throws XMLStreamException {
    long id = Long.parseLong(reader.getAttributeValue(null, ATTRIBUTE_NAME_ID));
    Info info = readInfo();

    // read the content of the node
    Map<String, Object> tags = new HashMap<>();
    List<Long> members = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
      switch (reader.getLocalName()) {
        case ELEMENT_NAME_TAG:
          readTag(tags);
          break;
        case ELEMENT_NAME_NODE_REFERENCE:
          readWayMember(members);
          break;
        default:
          readUnknownElement();
          break;
      }
    }

    return new Way(id, info, tags, members);
  }

  private void readWayMember(List<Long> members) throws XMLStreamException {
    Long member = Long.parseLong(reader.getAttributeValue(null, ATTRIBUTE_NAME_REF));
    members.add(member);
    reader.nextTag();
    reader.nextTag();
  }

  private Relation readRelation() throws XMLStreamException {
    long id = Long.parseLong(reader.getAttributeValue(null, ATTRIBUTE_NAME_ID));
    Info info = readInfo();

    // read the content of the node
    Map<String, Object> tags = new HashMap<>();
    List<Member> members = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
      switch (reader.getLocalName()) {
        case ELEMENT_NAME_TAG:
          readTag(tags);
          break;
        case ELEMENT_NAME_MEMBER:
          readRelationMember(members);
          break;
        default:
          readUnknownElement();
          break;
      }
    }

    return new Relation(id, info, tags, members);
  }

  private void readRelationMember(List<Member> members) throws XMLStreamException {
    long id = Long.parseLong(reader.getAttributeValue(null, ATTRIBUTE_NAME_REF));
    Member.MemberType type = Member.MemberType
        .valueOf(reader.getAttributeValue(null, ATTRIBUTE_NAME_TYPE).toUpperCase());
    String role = reader.getAttributeValue(null, ATTRIBUTE_NAME_ROLE);
    members.add(new Member(id, type, role));
    reader.nextTag();
    reader.nextTag();
  }

  private Info readInfo() {
    String versionValue = reader.getAttributeValue(null, ATTRIBUTE_NAME_VERSION);
    int version = versionValue != null ? Ints.tryParse(versionValue) : 0;
    String timestampValue = reader.getAttributeValue(null, ATTRIBUTE_NAME_TIMESTAMP);
    LocalDateTime timestamp =
        timestampValue != null ? LocalDateTime.parse(timestampValue, format) : null;
    String changesetValue = reader.getAttributeValue(null, ATTRIBUTE_NAME_CHANGESET_ID);
    long changeset = changesetValue != null ? Longs.tryParse(changesetValue) : -1;
    String uidValue = reader.getAttributeValue(null, ATTRIBUTE_NAME_USER_ID);
    int uid = uidValue != null && Ints.tryParse(uidValue) != null ? Ints.tryParse(uidValue) : -1;
    return new Info(version, timestamp, changeset, uid);
  }

  private void readTag(Map<String, Object> tags) throws XMLStreamException {
    String name = reader.getAttributeValue(null, ATTRIBUTE_NAME_KEY);
    String value = reader.getAttributeValue(null, ATTRIBUTE_NAME_VALUE);
    tags.put(name, value);
    reader.nextTag();
    reader.nextTag();
  }

  private void readUnknownElement() throws XMLStreamException {
    int level = 0;
    do {
      if (reader.getEventType() == XMLStreamConstants.START_ELEMENT) {
        level++;
      } else if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
        level--;
      }
      reader.next();
    } while (level > 0);
  }

  @Override
  public Spliterator<Entity> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return NONNULL | ORDERED | CONCURRENT;
  }
}
