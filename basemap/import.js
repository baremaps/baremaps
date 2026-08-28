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
import config from "./config.js";

export default {
  "steps": [
    {
      "id": "openstreetmap-water-polygons",
      "needs": [],
      "tasks": [
        {
          "type": "DownloadUrl",
          "source": "https://osmdata.openstreetmap.de/download/water-polygons-split-3857.zip",
          "target": "data/water-polygons-split-3857.zip",
          "replaceExisting": false,
        },
        {
          "type": "DecompressFile",
          "source": "data/water-polygons-split-3857.zip",
          "target": "data",
          "compression": "zip"
        },
        {
          "type": "ImportShapefile",
          "file": "data/water-polygons-split-3857/water_polygons.shp",
          "database": config.database,
          "fileSrid": 3857,
          "databaseSrid": 3857
        },
      ]
    },
    {
      "id": "openstreetmap-simplified-water-polygons",
      "needs": [],
      "tasks": [
        {
          "type": "DownloadUrl",
          "source": "https://osmdata.openstreetmap.de/download/simplified-water-polygons-split-3857.zip",
          "target": "data/simplified-water-polygons-split-3857.zip",
          "replaceExisting": false,
        },
        {
          "type": "DecompressFile",
          "source": "data/simplified-water-polygons-split-3857.zip",
          "target": "data",
          "compression": "zip"
        },
        {
          "type": "ImportShapefile",
          "file": "data/simplified-water-polygons-split-3857/simplified_water_polygons.shp",
          "database": config.database,
          "fileSrid": 3857,
          "databaseSrid": 3857
        },
      ]
    },
    {
      "id": "openstreetmap-data",
      "needs": [],
      "tasks": [
        {
          "type": "DownloadUrl",
          "source": config.osmPbfUrl,
          "target": "data/data.osm.pbf",
          "replaceExisting": false,
        },
        {
          "type": "ImportOsmPbf",
          "file": "data/data.osm.pbf",
          "database": config.database,
          "databaseSrid": 3857,
          "replaceExisting": false,
          "truncateExisting": true,
        },
      ]
    }
  ]
}
