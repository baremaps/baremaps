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

package com.baremaps.cli.iploc;

import com.baremaps.cli.WebServer;
import com.baremaps.iploc.IpLocRepository;
import com.baremaps.server.IpLocResource;
import com.baremaps.utils.SqliteUtils;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start an IP to location web service.")
public class Serve implements Callable<Integer> {

  @Option(names = {"--database"}, paramLabel = "DATABASE",
      description = "The path of the SQLite database.", defaultValue = "iploc.db")
  private Path database;

  // The server listens on every interface, as it is often reached from outside the machine or the
  // container that runs it.
  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "0.0.0.0";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    var dataSource = SqliteUtils.createDataSource(database, true);
    new WebServer(host, port)
        .resource(new IpLocResource(new IpLocRepository(dataSource)))
        .files("/iploc", "index.html")
        .run();
    return 0;
  }
}
