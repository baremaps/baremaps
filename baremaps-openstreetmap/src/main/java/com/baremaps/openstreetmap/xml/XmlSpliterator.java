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

import com.baremaps.data.stream.StreamException;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Walks an OpenStreetMap XML file, handing each top-level element to a subclass.
 *
 * <p>
 * The file is read through a cursor that cannot be rewound, so it can only be traversed once and in
 * order. That is what makes these spliterators unsplittable and their size unknown until the end of
 * the file is reached.
 *
 * @param <T> the type of the elements produced
 */
abstract class XmlSpliterator<T> implements Spliterator<T> {

  final XmlElementReader elements;
  final XMLStreamReader reader;

  XmlSpliterator(InputStream input) {
    this.elements = new XmlElementReader(input);
    this.reader = elements.cursor();
  }

  @Override
  public boolean tryAdvance(Consumer<? super T> consumer) {
    try {
      if (!reader.hasNext()) {
        return false;
      }
      int event = reader.next();
      if (event == END_DOCUMENT) {
        return false;
      }
      if (event == START_ELEMENT) {
        readElement(consumer);
      }
      // Whitespace and comments advance the cursor without producing anything, so a true here
      // means "the file may hold more", not "the consumer was fed".
      return true;
    } catch (XMLStreamException e) {
      throw new StreamException(e);
    }
  }

  /**
   * Reads the element the cursor is on, feeding the consumer if that element produces one.
   *
   * @param consumer the consumer of the produced elements
   * @throws XMLStreamException if the file cannot be read
   */
  abstract void readElement(Consumer<? super T> consumer) throws XMLStreamException;

  @Override
  public Spliterator<T> trySplit() {
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
