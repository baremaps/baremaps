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

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.stream.StreamException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Traverses an OpenStreetMap change file (osc.xml) as a sequence of changes. */
public class XmlChangeSpliterator implements Spliterator<Change> {

  private final XmlElementReader elements;
  private final XMLStreamReader reader;

  public XmlChangeSpliterator(InputStream input) {
    this.elements = new XmlElementReader(input);
    this.reader = elements.cursor();
  }

  @Override
  public boolean tryAdvance(Consumer<? super Change> consumer) {
    try {
      if (!reader.hasNext()) {
        return false;
      }
      int event = reader.next();
      if (event == END_DOCUMENT) {
        return false;
      }
      if (event == START_ELEMENT) {
        switch (reader.getLocalName()) {
          case "osmChange" -> {
            // The root element: its children are the changes.
          }
          case "create" -> consumer.accept(readChange(Change.ChangeType.CREATE));
          case "modify" -> consumer.accept(readChange(Change.ChangeType.MODIFY));
          case "delete" -> consumer.accept(readChange(Change.ChangeType.DELETE));
          default -> throw new StreamException("Unexpected XML element: " + reader.getLocalName());
        }
      }
      return true;
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  private Change readChange(Change.ChangeType type) throws XMLStreamException {
    List<Entity> entities = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == START_ELEMENT) {
      entities.add(elements.readElement());
      reader.nextTag();
    }
    return new Change(type, entities);
  }

  @Override
  public Spliterator<Change> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return ORDERED | NONNULL;
  }
}
