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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.stream.AccumulatingConsumer;
import com.baremaps.openstreetmap.stream.HoldingConsumer;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Spliterator;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XmlChangeSpliteratorTest {

  @Test
  void tryAdvance() throws IOException {
    try (
        InputStream input = new GZIPInputStream(Files.newInputStream(TestFiles.SAMPLE_OSC_XML_2))) {
      Spliterator<Change> spliterator = new XmlChangeSpliterator(input);
      spliterator.forEachRemaining(Assertions::assertNotNull);
      assertFalse(spliterator.tryAdvance(new HoldingConsumer<>()));
    }
  }

  @Test
  void forEachRemaining() throws IOException {
    try (
        InputStream input = new GZIPInputStream(Files.newInputStream(TestFiles.SAMPLE_OSC_XML_2))) {
      Spliterator<Change> spliterator = new XmlChangeSpliterator(input);
      AccumulatingConsumer<Change> accumulator = new AccumulatingConsumer<>();
      spliterator.forEachRemaining(accumulator);
      assertEquals(5, accumulator.values().size());
      assertEquals(36, accumulator.values().stream()
          .flatMap(change -> change.entities().stream())
          .toList().size());
    }
  }
}
