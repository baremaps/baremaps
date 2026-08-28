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

import com.baremaps.workflow.Task;
import com.baremaps.workflow.WorkflowContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads a file from a URL.
 */
public class DownloadUrl implements Task {

  private static final Logger logger = LoggerFactory.getLogger(DownloadUrl.class);

  private static final String PROTOCOL_FTP = "ftp";

  private static final String PROTOCOL_HTTP = "http";

  private static final String PROTOCOL_HTTPS = "https";

  private String source;

  private Path target;

  private boolean replaceExisting = true;

  /**
   * Constructs a {@code DownloadUrl}.
   */
  public DownloadUrl() {

  }

  /**
   * Constructs an {@code DownloadUrl}.
   *
   * @param source the url
   * @param target the path
   * @param replaceExisting whether to replace existing files
   */
  public DownloadUrl(String source, Path target, boolean replaceExisting) {
    this.source = source;
    this.target = target;
    this.replaceExisting = replaceExisting;
  }

  @Override
  public void execute(WorkflowContext context) throws Exception {
    var sourceURL = new URL(source);
    var targetPath = target.toAbsolutePath();

    if (Files.exists(targetPath) && !replaceExisting) {
      logger.info("Skipping download of {} to {}", source, target);
      return;
    }

    if (isHttp(sourceURL)) {
      var get = (HttpURLConnection) sourceURL.openConnection();
      get.setInstanceFollowRedirects(true);
      get.setRequestMethod("GET");
      urlDownloadToFile(get, targetPath);
      get.disconnect();
    } else if (isFtp(sourceURL)) {
      urlDownloadToFile(sourceURL.openConnection(), targetPath);
    } else {
      throw new IllegalArgumentException("Unsupported URL protocol (supported: http(s)/ftp)");
    }
  }

  private static boolean isHttp(URL url) {
    return url.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP) ||
        url.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS);
  }

  private static boolean isFtp(URL url) {
    return url.getProtocol().equalsIgnoreCase(PROTOCOL_FTP);
  }

  private static void urlDownloadToFile(URLConnection url, Path targetPath) throws IOException {
    try (var inputStream = url.getInputStream()) {
      Files.createDirectories(targetPath.toAbsolutePath().getParent());
      Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new StringJoiner(", ", DownloadUrl.class.getSimpleName() + "[", "]")
        .add("source='" + source + "'")
        .add("target=" + target)
        .add("replaceExisting=" + replaceExisting)
        .toString();
  }
}
