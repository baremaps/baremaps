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



import com.baremaps.rpsl.RpslObject.RpslAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

class RpslSpliterator implements Spliterator<RpslObject> {

  private final Spliterator<String> lineSpliterator;
  private String currentLine;

  public RpslSpliterator(Spliterator<String> lineSpliterator) {
    this.lineSpliterator = lineSpliterator;
  }

  @Override
  public boolean tryAdvance(Consumer<? super RpslObject> consumer) {
    List<RpslAttribute> attributes = new ArrayList<>();
    String attributeName = null;
    StringBuilder attributeValue = new StringBuilder();

    while (advanceLine()) {
      if (currentLine.isEmpty()) {
        if (!attributes.isEmpty()) {
          attributes.add(new RpslAttribute(attributeName, attributeValue.toString()));
          consumer.accept(new RpslObject(attributes));
          return true;
        }
        continue;
      }

      if (currentLine.startsWith("#") || currentLine.startsWith("%")) {
        continue; // Skip comments
      }

      if (currentLine.startsWith(" ") || currentLine.startsWith("+")) {
        // Continuation of the previous attribute's value
        attributeValue.append('\n').append(currentLine.trim());
      } else {
        // New attribute
        if (attributeName != null) {
          attributes.add(new RpslAttribute(attributeName, attributeValue.toString()));
        }
        int index = currentLine.indexOf(':');
        if (index < 0) {
          // Handle error: malformed attribute line
          continue;
        }
        attributeName = currentLine.substring(0, index).trim();
        attributeValue = new StringBuilder(currentLine.substring(index + 1).trim());
      }
    }

    if (attributeName != null) {
      attributes.add(new RpslAttribute(attributeName, attributeValue.toString()));
      consumer.accept(new RpslObject(attributes));
      return true;
    }

    return false;
  }

  private boolean advanceLine() {
    return lineSpliterator.tryAdvance(line -> currentLine = line);
  }

  @Override
  public Spliterator<RpslObject> trySplit() {
    return null; // Cannot be split further
  }

  @Override
  public long estimateSize() {
    return lineSpliterator.estimateSize();
  }

  @Override
  public int characteristics() {
    return lineSpliterator.characteristics();
  }
}
