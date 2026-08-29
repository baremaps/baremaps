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

package com.baremaps.calcite.openstreetmap;

import com.baremaps.calcite.FileSchema;
import java.io.File;
import java.io.IOException;
import java.util.List;

/** A schema exposing an OpenStreetMap file, or the OSM files of a directory, as tables. */
public class OpenStreetMapSchema extends FileSchema {

  public OpenStreetMapSchema(File fileOrDirectory) throws IOException {
    super(fileOrDirectory, List.of(".pbf", ".xml", ".osm"), OpenStreetMapTable::new);
  }
}
