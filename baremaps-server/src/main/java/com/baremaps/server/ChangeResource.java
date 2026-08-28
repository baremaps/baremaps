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

package com.baremaps.server;


import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import com.baremaps.config.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linecorp.armeria.common.sse.ServerSentEvent;
import com.linecorp.armeria.server.ServiceRequestContext;
import com.linecorp.armeria.server.annotation.Get;
import com.linecorp.armeria.server.annotation.ProducesEventStream;
import java.io.IOException;
import java.nio.file.*;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

/**
 * A resource that provides the changes in the tileset and style.
 */
public final class ChangeResource {

  private static final Logger logger = LoggerFactory.getLogger(ChangeResource.class);

  private final ConfigReader configReader = new ConfigReader();

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final Path tileset;

  private final Path style;

  private final Thread thread;

  private final Sinks.Many<ServerSentEvent> changes = Sinks.many().multicast().directBestEffort();

  /**
   * Constructs a {@code ChangeResource}.
   * 
   * @param tileset the path to the tileset
   * @param style the path to the style
   */
  public ChangeResource(Path tileset, Path style) {
    this.tileset = tileset;
    this.style = style;
    this.thread = new Thread(new ChangeListener());
    this.thread.start();
  }

  /**
   * Returns the changes in the tileset and style.
   * 
   * @param ctx the service request context
   */
  @Get("/changes")
  @ProducesEventStream
  public Publisher<ServerSentEvent> changes(ServiceRequestContext ctx) {
    ctx.clearRequestTimeout();
    return changes.asFlux();
  }

  /**
   * A {@code Runnable} that detects changes in the tileset and style.
   */
  class ChangeListener implements Runnable {

    /**
     * Detects changes in the tileset and style and broadcasts them.
     */
    @Override
    @SuppressWarnings("squid:S3776")
    public void run() {
      try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
        if (tileset != null && Files.exists(tileset)) {
          registerRecursively(tileset.toAbsolutePath().getParent(), watchService);
        }
        if (style != null && Files.exists(style)) {
          registerRecursively(style.toAbsolutePath().getParent(), watchService);
        }
        WatchKey key;
        while ((key = watchService.take()) != null) {
          Path dir = (Path) key.watchable();

          for (WatchEvent<?> event : key.pollEvents()) {
            Path path = dir.resolve((Path) event.context()).toAbsolutePath();
            logger.info("Change detected in {}", path);

            if (style != null) {
              var value = configReader.read(style);
              var styleObjectNode = objectMapper.readValue(value, ObjectNode.class);

              // reload the page if changes affected the tileset
              if (tileset != null && path.endsWith(tileset.getFileName())) {
                styleObjectNode.put("reload", true);
              }

              // broadcast the changes
              changes.tryEmitNext(ServerSentEvent.ofData(styleObjectNode.toString()));
            }
          }
          key.reset();
        }
      } catch (InterruptedException | IOException e) {
        logger.error(e.getMessage());
        Thread.currentThread().interrupt();
      }
    }

    /**
     * Registers a directory and its sub-directories with the watch service.
     *
     * @param directory the directory
     * @param watchService the watch service
     * @throws IOException
     */
    private void registerRecursively(Path directory, WatchService watchService) throws IOException {
      try (var directories = Files.walk(directory)) {
        directories.filter(Files::isDirectory).forEach(path -> {
          try {
            path.register(watchService, ENTRY_MODIFY);
          } catch (IOException e) {
            logger.error(e.getMessage());
          }
        });
      }
    }
  }
}
