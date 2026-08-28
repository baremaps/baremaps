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

import com.baremaps.openstreetmap.model.DataBlock;
import com.baremaps.openstreetmap.model.Info;
import com.baremaps.openstreetmap.model.Member;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.osm.binary.Osmformat;
import com.baremaps.osm.binary.Osmformat.DenseNodes;
import com.baremaps.osm.binary.Osmformat.PrimitiveGroup;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Decodes a data blob of an OpenStreetMap PBF file.
 *
 * <p>
 * The format stores coordinates as integers scaled by a per-block granularity and offset, dates as
 * integers scaled by a per-block date granularity, and strings by index in a per-block string
 * table. Dense nodes additionally delta-encode every field against the previous node, as do way
 * node references and relation member ids.
 */
final class DataBlockReader {

  private final Osmformat.PrimitiveBlock block;
  private final int granularity;
  private final int dateGranularity;
  private final long latOffset;
  private final long lonOffset;
  private final String[] stringTable;

  private DataBlockReader(Osmformat.PrimitiveBlock block) {
    this.block = block;
    this.granularity = block.getGranularity();
    this.latOffset = block.getLatOffset();
    this.lonOffset = block.getLonOffset();
    this.dateGranularity = block.getDateGranularity();
    this.stringTable = new String[block.getStringtable().getSCount()];
    for (int i = 0; i < stringTable.length; i++) {
      stringTable[i] = block.getStringtable().getS(i).toStringUtf8();
    }
  }

  static DataBlock read(Blob blob) throws DataFormatException, InvalidProtocolBufferException {
    return new DataBlockReader(Osmformat.PrimitiveBlock.parseFrom(blob.data())).read();
  }

  private DataBlock read() {
    List<Node> nodes = new ArrayList<>();
    List<Way> ways = new ArrayList<>();
    List<Relation> relations = new ArrayList<>();
    for (PrimitiveGroup group : block.getPrimitivegroupList()) {
      readDenseNodes(group.getDense(), nodes);
      for (Osmformat.Node node : group.getNodesList()) {
        nodes.add(readNode(node));
      }
      for (Osmformat.Way way : group.getWaysList()) {
        ways.add(readWay(way));
      }
      for (Osmformat.Relation relation : group.getRelationsList()) {
        relations.add(readRelation(relation));
      }
    }
    return new DataBlock(nodes, ways, relations);
  }

  private void readDenseNodes(DenseNodes denseNodes, List<Node> nodes) {
    Osmformat.DenseInfo denseInfo = denseNodes.getDenseinfo();
    long id = 0;
    long lat = 0;
    long lon = 0;
    long timestamp = 0;
    long changeset = 0;
    int uid = 0;
    // Index into the flat keys/vals array, where each node's pairs end with a 0.
    int kv = 0;
    for (int i = 0; i < denseNodes.getIdCount(); i++) {
      id += denseNodes.getId(i);
      lat += denseNodes.getLat(i);
      lon += denseNodes.getLon(i);
      uid += denseInfo.getUid(i);
      timestamp += denseInfo.getTimestamp(i);
      changeset += denseInfo.getChangeset(i);
      Map<String, Object> tags = new HashMap<>();
      if (denseNodes.getKeysValsCount() > 0) {
        while (denseNodes.getKeysVals(kv) != 0) {
          tags.put(stringTable[denseNodes.getKeysVals(kv++)],
              stringTable[denseNodes.getKeysVals(kv++)]);
        }
        kv++;
      }
      Info info = new Info(denseInfo.getVersion(i), toTimestamp(timestamp), changeset, uid);
      nodes.add(new Node(id, info, tags, toLon(lon), toLat(lat)));
    }
  }

  private Node readNode(Osmformat.Node node) {
    return new Node(node.getId(), toInfo(node.getInfo()),
        toTags(node.getKeysList(), node.getValsList()), toLon(node.getLon()), toLat(node.getLat()));
  }

  private Way readWay(Osmformat.Way way) {
    long ref = 0;
    List<Long> nodes = new ArrayList<>(way.getRefsCount());
    for (int i = 0; i < way.getRefsCount(); i++) {
      ref += way.getRefs(i);
      nodes.add(ref);
    }
    return new Way(way.getId(), toInfo(way.getInfo()), toTags(way.getKeysList(), way.getValsList()),
        nodes);
  }

  private Relation readRelation(Osmformat.Relation relation) {
    long ref = 0;
    List<Member> members = new ArrayList<>(relation.getMemidsCount());
    for (int i = 0; i < relation.getMemidsCount(); i++) {
      ref += relation.getMemids(i);
      String role = stringTable[relation.getRolesSid(i)];
      Member.MemberType type = Member.MemberType.forNumber(relation.getTypes(i).getNumber());
      members.add(new Member(ref, type, role));
    }
    return new Relation(relation.getId(), toInfo(relation.getInfo()),
        toTags(relation.getKeysList(), relation.getValsList()), members);
  }

  private Info toInfo(Osmformat.Info info) {
    return new Info(info.getVersion(), toTimestamp(info.getTimestamp()), info.getChangeset(),
        info.getUid());
  }

  private Map<String, Object> toTags(List<Integer> keys, List<Integer> vals) {
    Map<String, Object> tags = new HashMap<>();
    for (int i = 0; i < keys.size(); i++) {
      tags.put(stringTable[keys.get(i)], stringTable[vals.get(i)]);
    }
    return tags;
  }

  private double toLat(long lat) {
    return (granularity * lat + latOffset) * 1e-9;
  }

  private double toLon(long lon) {
    return (granularity * lon + lonOffset) * 1e-9;
  }

  private LocalDateTime toTimestamp(long timestamp) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateGranularity * timestamp),
        ZoneOffset.UTC);
  }
}
