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

package com.baremaps.cli.map;

import com.baremaps.tasks.ExportVectorTiles;
import com.baremaps.workflow.WorkflowContext;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "export",
    description = "Export vector tiles from the database.")
public class Export implements Callable<Integer> {

  @Option(names = {"--map"}, paramLabel = "MAP",
      description = "The map specification file, from which the style and the tileset are derived.")
  private Path map;

  @Option(names = {"--tileset"}, paramLabel = "TILESET",
      description = "The tileset file. Superseded by --map.")
  private Path tileset;

  @Option(names = {"--style"}, paramLabel = "STYLE",
      description = "The style file. Superseded by --map.")
  private Path style;

  @Option(names = {"--repository"}, paramLabel = "REPOSITORY", description = "The tile repository.",
      required = true)
  private Path repository;

  @Option(names = {"--format"}, paramLabel = "FORMAT",
      description = "The format of the repository (${COMPLETION-CANDIDATES}).")
  private ExportVectorTiles.Format format = ExportVectorTiles.Format.FILE;

  @Override
  public Integer call() throws Exception {
    MapInput.validate(map, tileset, style);
    var task = map != null
        ? new ExportVectorTiles(map.toAbsolutePath(), repository.toAbsolutePath(), format)
        : new ExportVectorTiles(tileset.toAbsolutePath(), style.toAbsolutePath(),
            repository.toAbsolutePath(), format);
    task.execute(new WorkflowContext());
    return 0;
  }
}
