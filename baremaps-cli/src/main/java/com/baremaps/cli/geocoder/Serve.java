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

package com.baremaps.cli.geocoder;

import com.baremaps.cli.WebServer;
import com.baremaps.server.GeocoderResource;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start a geocoder web service.")
public class Serve implements Callable<Integer> {

  @Option(
      names = {"--index"}, paramLabel = "INDEX", description = "The path to the lucene index.",
      required = true)
  private Path indexDirectory;

  // The server listens on every interface, as it is often reached from outside the machine or the
  // container that runs it.
  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "0.0.0.0";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {
    try (var directory = FSDirectory.open(indexDirectory);
        var searcherManager = new SearcherManager(directory, new SearcherFactory())) {
      new WebServer(host, port)
          .resource(new GeocoderResource(searcherManager))
          .files("/geocoder", "index.html")
          .run();
    }
    return 0;
  }
}
