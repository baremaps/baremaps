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

package com.baremaps.cli;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.linecorp.armeria.common.HttpHeaderNames;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.annotation.JacksonResponseConverterFunction;
import com.linecorp.armeria.server.cors.CorsService;
import com.linecorp.armeria.server.docs.DocService;
import com.linecorp.armeria.server.file.FileService;
import com.linecorp.armeria.server.file.HttpFile;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * The web server behind the commands that serve maps, tiles or search results.
 *
 * <p>
 * These commands differ by the resources they expose and by the files they ship with, and agree on
 * everything else: they answer in JSON, they are browsed from pages served by another origin, and
 * they document themselves. Those decisions are taken here so that a command only declares what it
 * serves.
 */
public class WebServer {

  private final com.linecorp.armeria.server.ServerBuilder builder = Server.builder();

  private final JacksonResponseConverterFunction jsonConverter =
      new JacksonResponseConverterFunction(objectMapper());

  /**
   * Constructs a {@code WebServer} bound to a socket address.
   *
   * @param host the host of the server
   * @param port the port of the server
   */
  public WebServer(String host, int port) {
    builder.http(new InetSocketAddress(host, port));
  }

  /**
   * Exposes an annotated resource at the root of the server.
   *
   * @param resource the annotated resource
   * @return this web server
   */
  public WebServer resource(Object resource) {
    builder.annotatedService(resource, jsonConverter);
    return this;
  }

  /**
   * Exposes an annotated resource under a path.
   *
   * @param path the path under which the resource is exposed
   * @param resource the annotated resource
   * @return this web server
   */
  public WebServer resource(String path, Object resource) {
    builder.annotatedService(path, resource, jsonConverter);
    return this;
  }

  /**
   * Serves a directory of the classpath at the root of the server, one of its files answering for
   * the root itself.
   *
   * @param directory the directory of the classpath, such as {@code /static}
   * @param index the file answering for {@code /}, such as {@code viewer.html}
   * @return this web server
   */
  public WebServer files(String directory, String index) {
    var classLoader = ClassLoader.getSystemClassLoader();
    builder.service("/", HttpFile.of(classLoader, directory + "/" + index).asService());
    builder.serviceUnder("/", FileService.of(classLoader, directory));
    return this;
  }

  /**
   * Serves a local directory under {@code /assets}. A null directory serves nothing, as the assets
   * that complement a style (glyphs, sprites) are optional.
   *
   * @param directory the directory of assets, possibly null
   * @return this web server
   */
  public WebServer assets(Path directory) {
    if (directory != null) {
      builder.serviceUnder("/assets", FileService.of(directory));
    }
    return this;
  }

  /** Starts the server and blocks until the JVM shuts down. */
  public void run() {
    // The pages that consume these services are served by a development server, a text editor or a
    // file, and therefore rarely share the origin of the server.
    builder.decorator(CorsService.builderForAnyOrigin()
        .allowAllRequestHeaders(true)
        .allowRequestMethods(
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS,
            HttpMethod.HEAD)
        .allowCredentials()
        .exposeHeaders(HttpHeaderNames.LOCATION)
        .newDecorator());

    builder.serviceUnder("/docs", new DocService());

    // Neither header tells a map viewer anything it uses.
    builder.disableServerHeader();
    builder.disableDateHeader();

    var server = builder.build();
    server.start().join();
    server.closeOnJvmShutdown().join();
  }
}
