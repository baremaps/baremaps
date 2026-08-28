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

package com.baremaps.data.algorithm;

import com.baremaps.data.collection.DataList;
import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.hprtree.HPRtree;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.roaringbitmap.longlong.Roaring64Bitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class that streams the union of spatially connected geometries.
 */
public class UnionStream {

  private static final Logger logger = LoggerFactory.getLogger(UnionStream.class);

  private final DataList<Geometry> list;

  /**
   * Creates a new union stream.
   *
   * @param list the list of geometries to union.
   */
  public UnionStream(DataList<Geometry> list) {
    this.list = list;
  }

  /**
   * Performs a spatial join of the geometries and returns a stream of unions.
   * 
   * @return a stream of unions.
   */
  public Stream<Geometry> union() {
    // Use a bitmap to keep track of the geometries that have been visited
    var visited = new Roaring64Bitmap();

    // Create a spatial index of the geometries
    HPRtree tree = new HPRtree();
    for (int i = 0; i < list.size(); i++) {
      tree.insert(list.get(i).getEnvelopeInternal(), i);
    }
    tree.build();

    // Create a stream of geometry unions that are spatially connected
    var stream = LongStream.range(0, list.size()).mapToObj(i -> {
      // Skip the geometries that have already been visited
      if (visited.contains(i)) {
        return null;
      }
      visited.add(i);

      // Initialize an accumulator and a stack to visit neighbors
      var accumulator = new ArrayList<Geometry>();
      var stack = new ArrayDeque<Geometry>();

      // Add the current geometry to the accumulator and the stack
      var g = list.get(i);
      accumulator.add(g);
      stack.push(g);

      // Visit all neighbors that are spatially connected
      while (!stack.isEmpty()) {
        var g1 = stack.pop();
        tree.query(g1.getEnvelopeInternal(), o -> {
          var j = (int) o;
          if (visited.contains(j)) {
            return;
          }
          visited.add(j);
          var g2 = list.get(j);
          try {
            // If the geometries are not spatially disjoint, add them to the accumulator and the
            // stack,
            // so that their neighbors can be visited
            if (g1.intersects(g2)) {
              accumulator.add(g2);
              stack.push(g2);
            }
          } catch (TopologyException e) {
            // This should occur only if the geometries are invalid,
            // or in the case of a bug in JTS.
            logger.warn("Failed to union geometries", e);
            logger.warn("Geometry 1: {}", g1);
            logger.warn("Geometry 2: {}", g2);
          }
        });
      }

      // Union the geometries in the accumulator
      return new UnaryUnionOp(accumulator).union();
    });

    // Filter out the null values
    return stream.filter(Objects::nonNull);
  }
}
