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

import com.baremaps.openstreetmap.model.Entity;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;

/** Traverses an OpenStreetMap XML file (osm.xml) as a sequence of entities. */
public class XmlEntitySpliterator extends XmlSpliterator<Entity> {

  public XmlEntitySpliterator(InputStream input) {
    super(input);
  }

  @Override
  void readElement(Consumer<? super Entity> consumer) throws XMLStreamException {
    switch (reader.getLocalName()) {
      case "osm" -> consumer.accept(elements.readHeader());
      case "bound", "bounds" -> consumer.accept(elements.readBounds());
      case XmlElementReader.NODE, XmlElementReader.WAY, XmlElementReader.RELATION -> consumer
          .accept(elements.readElement());
      default -> elements.skipElement();
    }
  }
}
