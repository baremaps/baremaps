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

package com.baremaps.calcite.rpsl;

import com.baremaps.calcite.FileSchema;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A schema exposing an RPSL file, or the {@code .rpsl}/{@code .txt} files of a directory, as
 * tables.
 */
public class RpslSchema extends FileSchema {

  public RpslSchema(File fileOrDirectory) throws IOException {
    super(fileOrDirectory, List.of(".rpsl", ".txt"), RpslTable::new);
  }
}
