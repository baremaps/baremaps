/**
 Licensed under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
export default {
    "host": "http://localhost:9000",
    "database": "jdbc:postgresql://localhost:5432/baremaps?&user=baremaps&password=baremaps",
    "osmPbfUrl": "https://download.geofabrik.de/europe/switzerland-latest.osm.pbf",
    // The elevation the relief and the contours are traced from: an archive of terrarium tiles,
    // such as the ones Mapterhorn publishes. Download one, or an extract of one, to this path.
    // How deep it goes is read from the archive, so extracting a deeper one is all it takes.
    // https://mapterhorn.com/data-access/
    "dem": "data/mapterhorn.pmtiles",
    "center": [0, 0],
    "bounds": [-180, -85, 180, 85],
    "zoom": 2,
}