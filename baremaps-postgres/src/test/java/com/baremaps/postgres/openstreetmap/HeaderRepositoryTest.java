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

import static com.baremaps.postgres.Constants.HEADER_0;
import static com.baremaps.postgres.Constants.HEADER_1;
import static com.baremaps.postgres.Constants.HEADER_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.baremaps.openstreetmap.model.Header;
import com.baremaps.testing.PostgresRepositoryTest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HeaderRepositoryTest extends PostgresRepositoryTest {

  HeaderRepository headerRepository;

  @BeforeEach
  void init() {
    headerRepository = new HeaderRepository(dataSource());
  }

  @Test
  @Tag("integration")
  void selectAll() throws RepositoryException {
    List<Header> headers = Arrays.asList(HEADER_0, HEADER_1, HEADER_2);
    headerRepository.put(headers);
    assertEquals(3, headerRepository.selectAll().size());
  }

  @Test
  @Tag("integration")
  void selectLatest() throws RepositoryException {
    List<Header> headers = Arrays.asList(HEADER_0, HEADER_1, HEADER_2);
    headerRepository.put(headers);
    assertEquals(HEADER_2, headerRepository.selectLatest());
  }

  @Test
  @Tag("integration")
  void insert() throws RepositoryException {
    headerRepository.put(HEADER_0);
    assertEquals(HEADER_0, headerRepository.get(HEADER_0.replicationSequenceNumber()));
  }

  @Test
  @Tag("integration")
  void insertAll() throws RepositoryException {
    List<Header> headers = Arrays.asList(HEADER_0, HEADER_1, HEADER_2);
    headerRepository.put(headers);
    assertIterableEquals(headers, headerRepository.get(
        headers.stream().map(Header::replicationSequenceNumber).toList()));
  }

  @Test
  @Tag("integration")
  void delete() throws RepositoryException {
    headerRepository.put(HEADER_0);
    headerRepository.delete(HEADER_0.replicationSequenceNumber());
    assertNull(headerRepository.get(HEADER_0.replicationSequenceNumber()));
  }

  @Test
  @Tag("integration")
  void deleteAll() throws RepositoryException {
    List<Header> headers = Arrays.asList(HEADER_0, HEADER_1, HEADER_2);
    headerRepository.put(headers);
    headerRepository.delete(
        headers.stream().map(Header::replicationSequenceNumber).toList());
    assertIterableEquals(Arrays.asList(null, null, null), headerRepository.get(
        headers.stream().map(Header::replicationSequenceNumber).toList()));
  }

  @Test
  @Tag("integration")
  void copy() throws RepositoryException {
    List<Header> headers = Arrays.asList(HEADER_0, HEADER_1, HEADER_2);
    headerRepository.copy(headers);
    assertIterableEquals(headers, headerRepository.get(
        headers.stream().map(Header::replicationSequenceNumber).toList()));
  }
}
