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

package com.baremaps.openstreetmap.utils;

import java.util.List;
import java.util.Map;

/**
 * A map whose lookups are cheaper in batches, such as one backed by a database where every
 * {@link #get(Object)} is a round trip. Geometry builders read the nodes of a way through
 * {@link #getAll(List)} when the map is one of these.
 */
public interface BatchMap<K, V> extends Map<K, V> {

  /** Returns the value of each key, in order, with {@code null} for the absent ones. */
  List<V> getAll(List<K> keys);
}
