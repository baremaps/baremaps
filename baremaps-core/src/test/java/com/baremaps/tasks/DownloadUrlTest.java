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

package com.baremaps.tasks;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baremaps.data.util.FileUtils;
import com.baremaps.workflow.WorkflowContext;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class DownloadUrlTest {

  @Test
  @Tag("integration")
  void execute() throws Exception {
    var file = File.createTempFile("test", ".tmp");
    file.deleteOnExit();
    var task = new DownloadUrl("https://raw.githubusercontent.com/baremaps/baremaps/main/README.md",
        file.toPath(), true);
    task.execute(new WorkflowContext());
    assertTrue(Files.readString(file.toPath()).contains("Baremaps"));
  }

  @Disabled("Test relies on third party resource, see TODO comment.")
  @Test
  @Tag("integration")
  void testDownloadFtp() throws Exception {
    var directory = Files.createTempDirectory("tmp_");
    var file = directory.resolve("file");
    // TODO: do not use a 3rd party server, replaces test URL to a baremaps owned test resource.
    var task = new DownloadUrl("ftp://whois.in.bell.ca/bell.db.gz",
        file, false);
    task.execute(new WorkflowContext());
    assertTrue(file.toFile().length() > 50, "file is less than 50 bytes");
    FileUtils.deleteRecursively(directory);
  }

  @Test
  @Tag("integration")
  void testDownloadUnsupportedProtocol() throws Exception {
    var directory = Files.createTempDirectory("tmp_");
    var file = directory.resolve("file");
    assertThrows(IllegalArgumentException.class, () -> {
      var task = new DownloadUrl("file://not-existing-file-243jhks",
          file, false);
      task.execute(new WorkflowContext());
    }, "Unsupported protocol throws IOException");
    FileUtils.deleteRecursively(directory);
  }

  @Test
  @Tag("integration")
  void executeFileThatDoesntExist() throws Exception {
    var directory = Files.createTempDirectory("tmp_");
    var file = directory.resolve("README.md");
    var task = new DownloadUrl("https://raw.githubusercontent.com/baremaps/baremaps/main/README.md",
        file.toAbsolutePath(), false);
    task.execute(new WorkflowContext());
    assertTrue(Files.readString(file).contains("Baremaps"));
    FileUtils.deleteRecursively(directory);
  }
}
