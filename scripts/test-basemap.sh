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

baremaps workflow execute --file import.js

echo ""
echo "--------------------------------------------------------------------"
echo "Start the Basemap server"
echo "--------------------------------------------------------------------"
echo ""

nohup baremaps map dev --tileset 'tileset.js' --style 'style.js' > /dev/null 2>&1 &
baremaps=$!

sleep 10

echo "Get the status of the root (/):"
curl -I http://localhost:9000/

echo "Get the status of the style (/style.json):"
curl -I http://localhost:9000/style.json

echo "Get the status of the tileset (/tiles.json):"
curl -I http://localhost:9000/tiles.json

echo "Get the status of a tile (/tiles/14/8625/5746.mvt):"
curl -I http://localhost:9000/tiles/1/1/1.mvt

echo "Get the status of an out-of-bound tile (/tiles/16/16398/10986.mvt):"
curl -I http://localhost:9000/tiles/16/16398/10986.mvt

kill $baremaps

echo ""
echo "--------------------------------------------------------------------"
echo "Export the Basemap tiles"
echo "--------------------------------------------------------------------"
echo ""

baremaps map export \
  --tileset 'tileset.js' \
  --style 'style.js' \
  --repository 'tiles/' \
  --format file

du -h tiles

baremaps map export \
  --tileset 'tileset.js' \
  --style 'style.js' \
  --repository 'tiles.mbtiles' \
  --format mbtiles

du -h tiles.mbtiles
