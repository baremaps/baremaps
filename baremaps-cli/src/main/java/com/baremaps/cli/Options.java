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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import picocli.CommandLine.Option;

/** The options understood by every command of the CLI. */
public class Options {

  /** The log levels a user can choose from, mapped onto the levels of the logging framework. */
  public enum LogLevel {
    TRACE(Level.TRACE),
    DEBUG(Level.DEBUG),
    INFO(Level.INFO),
    WARN(Level.WARN),
    ERROR(Level.ERROR),
    OFF(Level.OFF);

    private final Level level;

    LogLevel(Level level) {
      this.level = level;
    }
  }

  /**
   * Applies the log level while the command line is being parsed, hence before the command it
   * belongs to starts logging. The default level is the one configured in {@code log4j2.xml}.
   *
   * @param logLevel the log level
   */
  @Option(names = {"--log-level"}, paramLabel = "LOG_LEVEL",
      description = "The log level (${COMPLETION-CANDIDATES}).")
  public void setLogLevel(LogLevel logLevel) {
    Configurator.setRootLevel(logLevel.level);
  }
}
