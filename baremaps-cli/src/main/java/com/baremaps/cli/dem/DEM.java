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

package com.baremaps.cli.dem;



import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * A command group that contains commands for processing digital elevation models (DEMs).
 */
@Command(name = "dem", description = "Digital Elevation Models (DEMs) processing",
    subcommands = {
        Serve.class,
        VectorTileContours.class
    },
    sortOptions = false)
@SuppressWarnings("squid:S106")
public class DEM implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}
