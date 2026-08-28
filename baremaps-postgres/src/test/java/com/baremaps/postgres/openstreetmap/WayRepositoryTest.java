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
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.postgres.Constants;
import com.baremaps.testing.PostgresRepositoryTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class WayRepositoryTest extends PostgresRepositoryTest {

  WayRepository wayRepository;

  @BeforeEach
  void init() {
    wayRepository = new WayRepository(dataSource());
  }

  @Test
  @Tag("integration")
  void insert() throws RepositoryException {
    wayRepository.put(Constants.WAY_0);
    assertEquals(Constants.WAY_0, wayRepository.get(Constants.WAY_0.getId()));
  }

  @Test
  @Tag("integration")
  void insertAll() throws RepositoryException {
    List<Way> ways = Arrays.asList(Constants.WAY_0, Constants.WAY_1, Constants.WAY_2);
    wayRepository.put(ways);
    assertIterableEquals(ways,
        wayRepository.get(ways.stream().map(Element::getId).toList()));
  }

  @Test
  @Tag("integration")
  void delete() throws RepositoryException {
    wayRepository.put(Constants.WAY_0);
    wayRepository.delete(Constants.WAY_0.getId());
    assertNull(wayRepository.get(Constants.WAY_0.getId()));
  }

  @Test
  @Tag("integration")
  void deleteAll() throws RepositoryException {
    List<Way> ways = Arrays.asList(Constants.WAY_0, Constants.WAY_1, Constants.WAY_2);
    wayRepository.put(ways);
    wayRepository.delete(ways.stream().map(Element::getId).toList());
    assertIterableEquals(Arrays.asList(null, null, null),
        wayRepository.get(ways.stream().map(Element::getId).toList()));
  }

  @Test
  @Tag("integration")
  void copy() throws RepositoryException {
    List<Way> ways = Arrays.asList(Constants.WAY_0, Constants.WAY_1, Constants.WAY_2);
    wayRepository.copy(ways);
    assertIterableEquals(ways,
        wayRepository.get(ways.stream().map(Element::getId).toList()));
  }
}
