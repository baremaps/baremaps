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

package com.baremaps.workflow;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.tasks.DownloadUrl;
import com.baremaps.tasks.ImportOsmPbf;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

public class ObjectMapperTest {

  @Test
  public void test() throws IOException {
    var mapper = new ObjectMapper();

    // serialize the workflow
    var workflow1 =
        new Workflow(List.of(
            new Step("download", List.of(),
                List.of(new DownloadUrl(
                    "https://download.geofabrik.de/europe/liechtenstein-latest.osm.pbf",
                    Paths.get("liechtenstein-latest.osm.pbf"), false))),
            new Step("import", List.of("download"),
                List.of(new ImportOsmPbf(Paths.get("liechtenstein-latest.osm.pbf"),
                    "jdbc:postgresql://localhost:5432/baremaps?&user=baremaps&password=baremaps",
                    3857, true, true)))));
    var json = mapper.writeValueAsString(workflow1);
    assertTrue(json.contains(DownloadUrl.class.getSimpleName()));
    assertTrue(json.contains(ImportOsmPbf.class.getSimpleName()));

    // deserialize the workflow
    var workflow2 = mapper.readValue(json, Workflow.class);
    assertTrue(workflow2.getSteps().get(0).getTasks().get(0) instanceof DownloadUrl);
    assertTrue(workflow2.getSteps().get(1).getTasks().get(0) instanceof ImportOsmPbf);
  }
}
