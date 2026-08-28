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

package com.baremaps.openstreetmap;


import com.baremaps.openstreetmap.model.Bound;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.model.Header;
import com.baremaps.openstreetmap.model.Node;
import com.baremaps.openstreetmap.model.Relation;
import com.baremaps.openstreetmap.model.State;
import com.baremaps.openstreetmap.model.Way;
import com.baremaps.openstreetmap.pbf.PbfEntityReader;
import com.baremaps.openstreetmap.state.StateReader;
import com.baremaps.openstreetmap.xml.XmlEntityReader;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OsmSampleTest {

  @Test
  void sampleStateTxt() throws IOException {
    try (InputStream inputStream = Files.newInputStream(TestFiles.SAMPLE_STATE_TXT)) {
      State state = new StateReader().read(inputStream);
      Assertions.assertEquals(1, state.sequenceNumber());
      Assertions.assertEquals(LocalDateTime.parse("2000-01-01T00:00:00"), state.timestamp());
    }
  }

  @Test
  void sampleOsmPbf() throws IOException {
    try (InputStream inputStream = Files.newInputStream(TestFiles.SAMPLE_OSM_PBF)) {
      Stream<Entity> stream = new PbfEntityReader().read(inputStream);
      process(stream, 1, 1, 27, 7, 2);
    }
  }

  @Test
  void sampleOsmXml() throws IOException {
    try (InputStream inputStream = Files.newInputStream(TestFiles.SAMPLE_OSM_XML)) {
      Stream<Entity> stream = new XmlEntityReader().read(inputStream);
      process(stream, 1, 1, 27, 7, 2);
    }
  }

  void process(Stream<Entity> stream, long headerCount, long boundCount, long nodeCount,
      long wayCount, long relationCount) {
    AtomicLong headers = new AtomicLong(0);
    AtomicLong bounds = new AtomicLong(0);
    AtomicLong nodes = new AtomicLong(0);
    AtomicLong ways = new AtomicLong(0);
    AtomicLong relations = new AtomicLong(0);
    stream.forEach(entity -> {
      if (entity instanceof Header header) {
        Assertions.assertNotNull(header);
        Assertions.assertEquals("osmium/1.16.0", header.writingProgram());
        headers.incrementAndGet();
      } else if (entity instanceof Bound bound) {
        Assertions.assertNotNull(bound);
        Assertions.assertEquals(0.0, bound.minLat(), 0.000001);
        Assertions.assertEquals(0.0, bound.minLon(), 0.000001);
        Assertions.assertEquals(20.0, bound.maxLat(), 0.000001);
        Assertions.assertEquals(20.0, bound.maxLon(), 0.000001);
        bounds.incrementAndGet();
      } else if (entity instanceof Node node) {
        Assertions.assertNotNull(node);
        nodes.incrementAndGet();
      } else if (entity instanceof Way way) {
        Assertions.assertNotNull(way);
        ways.incrementAndGet();
      } else if (entity instanceof Relation relation) {
        Assertions.assertNotNull(relation);
        relations.incrementAndGet();
      }
    });
    Assertions.assertEquals(headerCount, headers.get());
    Assertions.assertEquals(boundCount, bounds.get());
    Assertions.assertEquals(nodeCount, nodes.get());
    Assertions.assertEquals(wayCount, ways.get());
    Assertions.assertEquals(relationCount, relations.get());
  }
}
