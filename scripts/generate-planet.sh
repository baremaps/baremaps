#!/bin/sh

# Licensed under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Start from the scripts directory
DIR="$(dirname "$0")"
cd "$DIR" || exit
cd ../basemap

echo ""
echo "--------------------------------------------------------------------"
echo "Execute the Basemap workflow"
echo "--------------------------------------------------------------------"
echo ""

rm -fr data tiles tiles.mbtiles

awk '{gsub("https://download.geofabrik.de/europe/switzerland-latest.osm.pbf", "https://planet.openstreetmap.org/pbf/planet-latest.osm.pbf"); print}' config.js > tmpfile && mv tmpfile config.js
awk '{gsub("6.02260949059, 45.7769477403, 10.4427014502, 47.8308275417", "-180, -85.0511, 180, 85.0511"); print}' config.js > tmpfile && mv tmpfile config.js

baremaps workflow execute --file workflow.js

echo ""
echo "--------------------------------------------------------------------"
echo "Export the Basemap tiles"
echo "--------------------------------------------------------------------"
echo ""

baremaps map export --tileset 'tileset.js' --repository 'tiles.mbtiles' --format mbtiles
