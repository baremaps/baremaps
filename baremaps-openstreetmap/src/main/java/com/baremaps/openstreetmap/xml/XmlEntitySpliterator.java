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

import com.baremaps.openstreetmap.model.Entity;
import com.baremaps.openstreetmap.stream.StreamException;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Traverses an OpenStreetMap XML file (osm.xml) as a sequence of entities. */
public class XmlEntitySpliterator implements Spliterator<Entity> {

  private final XmlElementReader elements;
  private final XMLStreamReader reader;

  public XmlEntitySpliterator(InputStream input) {
    this.elements = new XmlElementReader(input);
    this.reader = elements.cursor();
  }

  @Override
  public boolean tryAdvance(Consumer<? super Entity> consumer) {
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
          case "osm" -> consumer.accept(elements.readHeader());
          case "bound", "bounds" -> consumer.accept(elements.readBounds());
          case XmlElementReader.NODE, XmlElementReader.WAY, XmlElementReader.RELATION -> consumer
              .accept(elements.readElement());
          default -> elements.skipElement();
        }
      }
      return true;
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  @Override
  public Spliterator<Entity> trySplit() {
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
