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

package com.baremaps.data.collection;

import java.util.Map;

/**
 * A {@link Map} view over a collection that owns off-heap or memory-mapped storage: closing it
 * flushes the metadata the collection needs to be reopened and releases its memory.
 */
public interface CloseableMap<K, V> extends Map<K, V>, AutoCloseable {
}
