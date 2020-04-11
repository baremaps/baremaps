/*
 * Copyright (C) 2011 The Baremaps Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.baremaps.osm.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.baremaps.osm.TestUtils;
import com.baremaps.osm.store.Store.Entry;
import com.baremaps.core.postgis.PostgisHelper;
import com.baremaps.osm.model.Node;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class PostgisNodeStoreTest {

  public DataSource dataSource;

  public NodeTable nodeTable;

  @BeforeEach
  public void createTable() throws SQLException, IOException {
    dataSource = PostgisHelper.poolingDataSource(TestUtils.DATABASE_URL);
    nodeTable = new NodeTable(dataSource, TestUtils.NODE_BUILDER);
    try (Connection connection = dataSource.getConnection()) {
      PostgisHelper.executeScript(connection, "osm_create_extensions.sql");
      PostgisHelper.executeScript(connection, "osm_drop_tables.sql");
      PostgisHelper.executeScript(connection, "osm_create_tables.sql");
      PostgisHelper.executeScript(connection, "osm_create_primary_keys.sql");
    }
  }

  @Test
  @Tag("integration")
  public void put() {
    nodeTable.put(TestUtils.NODE_0);
    assertEquals(TestUtils.NODE_0, nodeTable.get(TestUtils.NODE_0.getId()));
  }

  @Test
  @Tag("integration")
  public void putAll() {
    List<NodeTable.Node> nodes = Arrays.asList(
        TestUtils.NODE_0, TestUtils.NODE_1, TestUtils.NODE_2);
    nodeTable.putAll(nodes);
    assertIterableEquals(
        nodes,
        nodeTable.getAll(nodes.stream().map(e -> e.getId()).collect(Collectors.toList())));
  }

  @Test
  @Tag("integration")
  public void delete() {
    nodeTable.put(TestUtils.NODE_0);
    nodeTable.delete(TestUtils.NODE_0.getId());
    assertThrows(IllegalArgumentException.class, () -> nodeTable.get(TestUtils.NODE_0.getId()));
  }

  @Test
  @Tag("integration")
  public void deleteAll() {
    List<NodeTable.Node> nodes = Arrays.asList(TestUtils.NODE_0, TestUtils.NODE_1, TestUtils.NODE_2);
    nodeTable.putAll(nodes);
    nodeTable.deleteAll(nodes.stream().map(e -> e.getId()).collect(Collectors.toList()));
    assertIterableEquals(
        Arrays.asList(null, null, null),
        nodeTable.getAll(nodes.stream().map(e -> e.getId()).collect(Collectors.toList())));
  }

  @Test
  @Tag("integration")
  public void importAll() {
    List<NodeTable.Node> nodes = Arrays.asList(TestUtils.NODE_0, TestUtils.NODE_1, TestUtils.NODE_2);
    nodeTable.importAll(nodes);
    assertIterableEquals(
        nodes,
        nodeTable.getAll(nodes.stream().map(e -> e.getId()).collect(Collectors.toList())));
  }
}