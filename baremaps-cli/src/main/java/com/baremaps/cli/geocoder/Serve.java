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


import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.server.GeocoderResource;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.annotation.JacksonResponseConverterFunction;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start a tile server with caching capabilities.")
@SuppressWarnings("squid:S106")
public class Serve implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Serve.class);

  @Option(
      names = {"--index"}, paramLabel = "INDEX", description = "The path to the lucene index.",
      required = true)
  private Path indexDirectory;

  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "localhost";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {

    try (
        var directory = FSDirectory.open(indexDirectory);
        var searcherManager = new SearcherManager(directory, new SearcherFactory())) {

      var serverBuilder = Server.builder();
      serverBuilder.http(port);

      var objectMapper = objectMapper();
      var jsonResponseConverter = new JacksonResponseConverterFunction(objectMapper);
      serverBuilder.annotatedService(new GeocoderResource(searcherManager), jsonResponseConverter);

      var index = HttpFile.of(ClassLoader.getSystemClassLoader(), "/geocoder/index.html");
      serverBuilder.service("/", index.asService());
      serverBuilder.serviceUnder("/",
          FileService.of(ClassLoader.getSystemClassLoader(), "/geocoder"));

      serverBuilder.decorator(CorsService.builderForAnyOrigin()
          .allowRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,
              HttpMethod.OPTIONS, HttpMethod.HEAD)
          .allowRequestHeaders(HttpHeaderNames.ORIGIN, HttpHeaderNames.CONTENT_TYPE,
              HttpHeaderNames.ACCEPT, HttpHeaderNames.AUTHORIZATION)
          .allowCredentials()
          .exposeHeaders(HttpHeaderNames.LOCATION)
          .newDecorator());

      serverBuilder.serviceUnder("/docs", new DocService());

      serverBuilder.disableServerHeader();
      serverBuilder.disableDateHeader();

      var server = serverBuilder.build();

      var startFuture = server.start();
      startFuture.join();

      var shutdownFuture = server.closeOnJvmShutdown();
      shutdownFuture.join();
    }

    return 0;
  }
}
