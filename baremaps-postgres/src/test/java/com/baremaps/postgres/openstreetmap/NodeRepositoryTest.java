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

package com.baremaps.postgres.openstreetmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.baremaps.openstreetmap.model.Element;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.postgres.Constants;
import com.baremaps.testing.PostgresRepositoryTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class NodeRepositoryTest extends PostgresRepositoryTest {

  NodeRepository nodeRepository;

  @BeforeEach
  void beforeEach() {
    nodeRepository = new NodeRepository(dataSource());
  }

  @Test
  @Tag("integration")
  void insert() throws RepositoryException {
    nodeRepository.put(Constants.NODE_0);
    assertEquals(Constants.NODE_0, nodeRepository.get(Constants.NODE_0.getId()));
  }

  @Test
  @Tag("integration")
  void insertAll() throws RepositoryException {
    List<Node> nodes = Arrays.asList(Constants.NODE_0, Constants.NODE_1, Constants.NODE_2);
    nodeRepository.put(nodes);
    assertIterableEquals(nodes,
        nodeRepository.get(nodes.stream().map(Element::getId).toList()));
  }

  @Test
  @Tag("integration")
  void delete() throws RepositoryException {
    nodeRepository.put(Constants.NODE_0);
    nodeRepository.delete(Constants.NODE_0.getId());
    assertNull(nodeRepository.get(Constants.NODE_0.getId()));
  }

  @Test
  @Tag("integration")
  void deleteAll() throws RepositoryException {
    List<Node> nodes = Arrays.asList(Constants.NODE_0, Constants.NODE_1, Constants.NODE_2);
    nodeRepository.put(nodes);
    nodeRepository.delete(nodes.stream().map(Element::getId).toList());
    assertIterableEquals(Arrays.asList(null, null, null),
        nodeRepository.get(nodes.stream().map(Element::getId).toList()));
  }

  @Test
  @Tag("integration")
  void copy() throws RepositoryException {
    List<Node> nodes = Arrays.asList(Constants.NODE_0, Constants.NODE_1, Constants.NODE_2);
    nodeRepository.copy(nodes);
    assertIterableEquals(nodes,
        nodeRepository.get(nodes.stream().map(Element::getId).toList()));
  }
}
