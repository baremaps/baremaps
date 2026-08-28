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


import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.baremaps.iploc.IpLocRepository;
import com.baremaps.server.IpLocResource;
import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.annotation.JacksonResponseConverterFunction;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "serve",
    description = "Start an IP to location web service.")
@SuppressWarnings("squid:S106")
public class Serve implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Serve.class);

  @Option(names = {"--database"}, paramLabel = "DATABASE",
      description = "The path of the SQLite database.", defaultValue = "iploc.db")
  private Path database;

  @Option(names = {"--host"}, paramLabel = "HOST", description = "The host of the server.")
  private String host = "localhost";

  @Option(names = {"--port"}, paramLabel = "PORT", description = "The port of the server.")
  private int port = 9000;

  @Override
  public Integer call() throws Exception {

    var jdbcUrl = String.format("JDBC:sqlite:%s", database.toString());

    var config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    var dataSource = new HikariDataSource(config);

    var ipLocRepository = new IpLocRepository(dataSource);

    var serverBuilder = Server.builder();
    serverBuilder.http(port);

    var objectMapper = objectMapper();
    var jsonResponseConverter = new JacksonResponseConverterFunction(objectMapper);
    serverBuilder.annotatedService(new IpLocResource(ipLocRepository), jsonResponseConverter);

    var index = HttpFile.of(ClassLoader.getSystemClassLoader(), "/iploc/index.html");
    serverBuilder.service("/", index.asService());
    serverBuilder.serviceUnder("/", FileService.of(ClassLoader.getSystemClassLoader(), "/iploc"));

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

    return 0;
  }
}
