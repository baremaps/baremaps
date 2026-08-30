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

package com.baremaps.config;

import static com.baremaps.utils.ObjectMapperUtils.objectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Reads the configuration files of Baremaps. A JSON file is read as is, whereas a JavaScript file
 * is evaluated first, which lets a configuration be assembled programmatically and read the
 * environment it runs in.
 */
public class ConfigReader {

  static {
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
  }

  private final ObjectMapper mapper = objectMapper();

  /**
   * Reads a configuration file and maps it to an object.
   *
   * @param path the path of the configuration file
   * @param type the type of the configuration object
   * @return the configuration object
   * @throws IOException if the configuration cannot be read
   */
  public <T> T read(Path path, Class<T> type) throws IOException {
    return mapper.readValue(read(path), type);
  }

  /**
   * Reads a configuration file as JSON text.
   *
   * @param path the path of the configuration file
   * @return the JSON representation of the configuration
   * @throws IOException if the configuration cannot be read
   */
  public String read(Path path) throws IOException {
    var extension = com.google.common.io.Files.getFileExtension(path.toString());
    return switch (extension) {
      case "js" -> eval(path);
      default -> Files.readString(path);
    };
  }

  private String eval(Path path) throws IOException {
    try (var context = Context.newBuilder("js")
        .option("js.esm-eval-returns-exports", "true")
        .option("js.scripting", "true")
        .allowExperimentalOptions(true)
        .allowIO(true)
        .build()) {

      // Expose the environment variables to the script
      var env = new HashMap<String, Object>(System.getenv());
      context.getBindings("js").putMember("env", ProxyObject.fromMap(env));

      var script = String.format("""
          import config from '%s';
          export default JSON.stringify(config);
          """, path.toAbsolutePath().toUri());
      var source = Source.newBuilder("js", new StringReader(script), "script.js")
          .mimeType("application/javascript+module")
          .build();
      var value = context.eval(source);
      return value.getMember("default").toString();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }
}
