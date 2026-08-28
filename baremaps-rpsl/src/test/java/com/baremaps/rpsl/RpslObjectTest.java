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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RpslObjectTest {

  private List<RpslObject> objects;

  @BeforeEach
  public void before() throws IOException {
    var file = TestFiles.RPSL_TXT;
    try (var input = Files.newInputStream(file)) {
      objects = new RpslReader().read(input).toList();
    }
  }

  @Test
  void type() {
    assertEquals("inetnum", objects.get(0).type());
    assertEquals("organisation", objects.get(8).type());
    assertEquals("inet6num", objects.get(9).type());
  }

  @Test
  void id() {
    assertEquals("0.0.0.0 - 0.0.0.255", objects.get(0).id());
    assertEquals("ORG-VDN2-RIPE", objects.get(8).id());
    assertEquals("2001:7fa:0:2::/64", objects.get(9).id());
  }

  @Test
  void attributes() {
    assertEquals(7, objects.get(0).attributes().size());
    assertEquals(5, objects.get(8).attributes().size());
    assertEquals(7, objects.get(9).attributes().size());
  }

  @Test
  void first() {
    RpslObject object = objects.get(0);
    assertEquals("Route de Cheseaux 1", object.first("descr").get());
  }

  @Test
  void all() {
    RpslObject object = objects.get(0);
    assertEquals(3, object.all("descr").size());
  }
}
