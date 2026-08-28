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



import com.baremaps.cli.Baremaps.VersionProvider;
import com.baremaps.cli.database.Database;
import com.baremaps.cli.dem.DEM;
import com.baremaps.cli.geocoder.Geocoder;
import com.baremaps.cli.iploc.IpLoc;
import com.baremaps.cli.map.Map;
import com.baremaps.cli.workflow.Workflow;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

@Command(
    name = "baremaps",
    description = "A toolkit for producing vector tiles.",
    versionProvider = VersionProvider.class,
    subcommands = {
        Workflow.class,
        Database.class,
        Map.class,
        Geocoder.class,
        IpLoc.class,
        DEM.class
    },
    sortOptions = false)
@SuppressWarnings("squid:S106")
public class Baremaps implements Callable<Integer> {

  private static final Logger logger = LoggerFactory.getLogger(Baremaps.class);

  @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version info.")
  boolean version;

  public static void main(String... args) {
    // Set the log level
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      String level = "";
      if (arg.equals("--log-level") && i + 1 < args.length) {
        level = args[i + 1].strip();
      } else if (arg.startsWith("--log-level=")) {
        level = arg.substring(12).strip();
      }
      if (!"".equals(level)) {
        Configurator.setRootLevel(Level.getLevel(level));
      }
    }

    // Execute the command
    CommandLine commandLine = new CommandLine(new Baremaps())
        .setCaseInsensitiveEnumValuesAllowed(true)
        .setUsageHelpLongOptionsMaxWidth(30)
        .addMixin("options", new Options());

    commandLine.execute(args);
  }

  @Override
  public Integer call() {
    CommandLine.usage(this, System.out);
    return 0;
  }


  static class VersionProvider implements IVersionProvider {

    public String[] getVersion() throws Exception {
      URL url = Baremaps.class.getResource("version.txt");
      if (url == null) {
        return new String[] {"No version.txt file found in the classpath."};
      }
      try (InputStream inputStream = url.openStream()) {
        Properties properties = new Properties();
        properties.load(inputStream);
        return new String[] {
            properties.getProperty("application") + " v" + properties.getProperty("version"),};
      }
    }
  }
}
