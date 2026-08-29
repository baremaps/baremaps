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

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import com.baremaps.data.stream.StreamException;
import com.baremaps.openstreetmap.model.Change;
import com.baremaps.openstreetmap.model.Change.ChangeType;
import com.baremaps.openstreetmap.model.Entity;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;

/** Traverses an OpenStreetMap change file (osc.xml) as a sequence of changes. */
public class XmlChangeSpliterator extends XmlSpliterator<Change> {

  public XmlChangeSpliterator(InputStream input) {
    super(input);
  }

  @Override
  void readElement(Consumer<? super Change> consumer) throws XMLStreamException {
    switch (reader.getLocalName()) {
      case "osmChange" -> {
        // The root element: its children are the changes.
      }
      case "create" -> consumer.accept(readChange(ChangeType.CREATE));
      case "modify" -> consumer.accept(readChange(ChangeType.MODIFY));
      case "delete" -> consumer.accept(readChange(ChangeType.DELETE));
      default -> throw new StreamException("Unexpected XML element: " + reader.getLocalName());
    }
  }

  private Change readChange(ChangeType type) throws XMLStreamException {
    List<Entity> entities = new ArrayList<>();
    reader.nextTag();
    while (reader.getEventType() == START_ELEMENT) {
      entities.add(elements.readElement());
      reader.nextTag();
    }
    return new Change(type, entities);
  }
}
