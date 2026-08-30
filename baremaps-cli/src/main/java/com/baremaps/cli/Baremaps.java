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

import com.baremaps.cli.database.Database;
import com.baremaps.cli.dem.DEM;
import com.baremaps.cli.geocoder.Geocoder;
import com.baremaps.cli.iploc.IpLoc;
import com.baremaps.cli.map.Map;
import com.baremaps.cli.workflow.Workflow;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

@Command(
    name = "baremaps",
    description = "A toolkit for producing vector tiles.",
    subcommands = {
        Workflow.class,
        Database.class,
        Map.class,
        Geocoder.class,
        IpLoc.class,
        DEM.class
    },
    sortOptions = false)
public class Baremaps extends CommandGroup {

  public static void main(String... args) {
    var commandLine = new CommandLine(new Baremaps())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setUsageHelpLongOptionsMaxWidth(30);
    configure(commandLine);

    // The exit code tells the shell, and the scripts and workflows built on it, whether the command
    // succeeded.
    System.exit(commandLine.execute(args));
  }

  /**
   * Gives a command and its subcommands the options that are understood at every level of the
   * hierarchy, so that a user never has to remember where an option belongs.
   *
   * @param commandLine the command to configure
   */
  private static void configure(CommandLine commandLine) {
    commandLine.addMixin("options", new Options());
    commandLine.getCommandSpec().mixinStandardHelpOptions(true);
    commandLine.getCommandSpec().versionProvider(new VersionProvider());
    commandLine.getSubcommands().values().forEach(Baremaps::configure);
  }

  /** Reports the version of the application, as recorded in the artifact at build time. */
  static class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
      var url = Baremaps.class.getResource("version.txt");
      if (url == null) {
        return new String[] {"No version.txt file found in the classpath."};
      }
      try (InputStream inputStream = url.openStream()) {
        var properties = new Properties();
        properties.load(inputStream);
        return new String[] {
            properties.getProperty("application") + " v" + properties.getProperty("version")};
      }
    }
  }
}
