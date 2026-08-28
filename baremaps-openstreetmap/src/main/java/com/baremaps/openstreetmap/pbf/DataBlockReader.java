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

package com.baremaps.openstreetmap.pbf;



import com.baremaps.openstreetmap.model.Blob;
import com.baremaps.openstreetmap.model.DataBlock;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.stream.StreamException;
import com.baremaps.osm.binary.Osmformat;
import com.baremaps.osm.binary.Osmformat.DenseNodes;
import com.baremaps.osm.binary.Osmformat.PrimitiveGroup;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;

/** A reader that extracts data blocks and entities from OpenStreetMap data blobs. */
public class DataBlockReader {

  private final Blob blob;

  private final Osmformat.PrimitiveBlock primitiveBlock;
  private final int granularity;
  private final int dateGranularity;
  private final long latOffset;
  private final long lonOffset;
  private final String[] stringTable;

  /**
   * Constructs a reader with the granularity, offsets and string table of the specified blob.
   *
   * @param blob the blob
   * @throws DataFormatException
   * @throws InvalidProtocolBufferException
   */
  public DataBlockReader(Blob blob) throws DataFormatException, InvalidProtocolBufferException {
    this.blob = blob;
    this.primitiveBlock = Osmformat.PrimitiveBlock.parseFrom(blob.data());
    this.granularity = primitiveBlock.getGranularity();
    this.latOffset = primitiveBlock.getLatOffset();
    this.lonOffset = primitiveBlock.getLonOffset();
    this.dateGranularity = primitiveBlock.getDateGranularity();
    this.stringTable = new String[primitiveBlock.getStringtable().getSCount()];
    for (int i = 0; i < stringTable.length; i++) {
      stringTable[i] = primitiveBlock.getStringtable().getS(i).toStringUtf8();
    }
  }

  /**
   * Returns the {@code DataBlock}.
   *
   * @return the data block
   */
  public DataBlock read() {
    List<Node> denseNodes = new ArrayList<>();
    readDenseNodes(denseNodes::add);
    List<Node> nodes = new ArrayList<>();
    readNodes(nodes::add);
    List<Way> ways = new ArrayList<>();
    readWays(ways::add);
    List<Relation> relations = new ArrayList<>();
    readRelations(relations::add);
    return new DataBlock(blob, denseNodes, nodes, ways, relations);
  }

  /**
   * Read the entities with the provided consumer.
   *
   * @param consumer the consumer
   */
  public void readEntities(Consumer<Entity> consumer) {
    readDenseNodes(consumer::accept);
    readNodes(consumer::accept);
    readWays(consumer::accept);
    readRelations(consumer::accept);
  }

  /**
   * Read the dense nodes with the provided consumer.
   *
   * @param consumer the consumer
   */
  public void readDenseNodes(Consumer<Node> consumer) {
    for (PrimitiveGroup group : primitiveBlock.getPrimitivegroupList()) {
      DenseNodes denseNodes = group.getDense();

      long id = 0;
      long lat = 0;
      long lon = 0;
      long timestamp = 0;
      long changeset = 0;
      int sid = 0;
      int uid = 0;

      // Index into the keysvals array.
      int j = 0;
      for (int i = 0; i < denseNodes.getIdCount(); i++) {
        id = denseNodes.getId(i) + id;

        Osmformat.DenseInfo denseInfo = denseNodes.getDenseinfo();
        int version = denseInfo.getVersion(i);
        uid = denseInfo.getUid(i) + uid;
        sid = denseInfo.getUserSid(i) + sid;
        timestamp = denseInfo.getTimestamp(i) + timestamp;
        changeset = denseInfo.getChangeset(i) + changeset;
        lat = denseNodes.getLat(i) + lat;
        lon = denseNodes.getLon(i) + lon;

        // If empty, assume that nothing here has keys or vals.
        Map<String, Object> tags = new HashMap<>();
        if (denseNodes.getKeysValsCount() > 0) {
          while (denseNodes.getKeysVals(j) != 0) {
            int keyid = denseNodes.getKeysVals(j++);
            int valid = denseNodes.getKeysVals(j++);
            tags.put(getString(keyid), getString(valid));
          }
          j++; // Skip over the '0' delimiter.
        }

        Info info = new Info(version, getTimestamp(timestamp), changeset, uid);
        consumer.accept(new Node(id, info, tags, getLon(lon), getLat(lat)));
      }
    }
  }

  /**
   * Read the nodes with the provided consumer.
   *
   * @param consumer the consumer
   */
  public void readNodes(Consumer<Node> consumer) {
    for (PrimitiveGroup group : primitiveBlock.getPrimitivegroupList()) {
      for (Osmformat.Node node : group.getNodesList()) {
        long id = node.getId();
        int version = node.getInfo().getVersion();
        LocalDateTime timestamp = getTimestamp(node.getInfo().getTimestamp());
        long changeset = node.getInfo().getChangeset();
        int uid = node.getInfo().getUid();
        Map<String, Object> tags = new HashMap<>();
        for (int t = 0; t < node.getKeysList().size(); t++) {
          tags.put(getString(node.getKeysList().get(t)), getString(node.getKeysList().get(t)));
        }
        double lon = getLon(node.getLon());
        double lat = getLat(node.getLat());

        Info info = new Info(version, timestamp, changeset, uid);
        consumer.accept(new Node(id, info, tags, lon, lat));
      }
    }
  }

  /**
   * Read the ways with the provided consumer.
   *
   * @param consumer the consumer
   */
  public void readWays(Consumer<Way> consumer) {
    for (PrimitiveGroup group : primitiveBlock.getPrimitivegroupList()) {
      for (Osmformat.Way way : group.getWaysList()) {
        long id = way.getId();
        int version = way.getInfo().getVersion();
        LocalDateTime timestamp = getTimestamp(way.getInfo().getTimestamp());
        long changeset = way.getInfo().getChangeset();
        int uid = way.getInfo().getUid();
        Map<String, Object> tags = getTags(way.getKeysList(), way.getValsList());
        long nid = 0;
        List<Long> nodes = new ArrayList<>();
        for (int index = 0; index < way.getRefsCount(); index++) {
          nid = nid + way.getRefs(index);
          nodes.add(nid);
        }

        Info info = new Info(version, timestamp, changeset, uid);
        consumer.accept(new Way(id, info, tags, nodes));
      }
    }
  }

  /**
   * Read the relations with the provided consumer.
   *
   * @param consumer the consumer
   */
  public void readRelations(Consumer<Relation> consumer) {
    for (PrimitiveGroup group : primitiveBlock.getPrimitivegroupList()) {
      for (Osmformat.Relation relation : group.getRelationsList()) {
        long id = relation.getId();
        int version = relation.getInfo().getVersion();
        LocalDateTime timestamp = getTimestamp(relation.getInfo().getTimestamp());
        long changeset = relation.getInfo().getChangeset();
        int uid = relation.getInfo().getUid();
        Map<String, Object> tags = getTags(relation.getKeysList(), relation.getValsList());

        long mid = 0;
        List<Member> members = new ArrayList<>();
        for (int j = 0; j < relation.getMemidsCount(); j++) {
          mid = mid + relation.getMemids(j);
          String role = getString(relation.getRolesSid(j));
          Member.MemberType type = type(relation.getTypes(j));
          members.add(new Member(mid, type, role));
        }

        Info info = new Info(version, timestamp, changeset, uid);
        consumer.accept(new Relation(id, info, tags, members));
      }
    }
  }

  private Member.MemberType type(Osmformat.Relation.MemberType type) {
    switch (type) {
      case NODE:
        return Member.MemberType.NODE;
      case WAY:
        return Member.MemberType.WAY;
      case RELATION:
        return Member.MemberType.RELATION;
      default:
        throw new UnsupportedOperationException();
    }
  }

  private double getLat(long lat) {
    return (granularity * lat + latOffset) * .000000001;
  }

  private double getLon(long lon) {
    return (granularity * lon + lonOffset) * .000000001;
  }

  private LocalDateTime getTimestamp(long timestamp) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateGranularity * timestamp),
        TimeZone.getDefault().toZoneId());
  }

  private Map<String, Object> getTags(List<Integer> keys, List<Integer> vals) {
    Map<String, Object> tags = new HashMap<>();
    for (int t = 0; t < keys.size(); t++) {
      tags.put(getString(keys.get(t)), getString(vals.get(t)));
    }
    return tags;
  }

  private String getString(int id) {
    return stringTable[id];
  }

  /**
   * Reads the provided data {@code Blob} and returns the corresponding {@code DataBlock}.
   *
   * @param blob the data blob
   * @return the data block
   */
  public static DataBlock read(Blob blob) {
    try {
      return new DataBlockReader(blob).read();
    } catch (DataFormatException | InvalidProtocolBufferException e) {
      throw new StreamException(e);
    }
  }
}
