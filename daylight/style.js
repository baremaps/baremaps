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


import background from "../basemap/layers/background/style.js";
import aeroway_line from "../basemap/layers/aeroway/line.js";
import aeroway_fill from "../basemap/layers/aeroway/fill.js";
import amenity_background from "../basemap/layers/amenity/background.js";
import amenity_fountain from "../basemap/layers/amenity/fountain.js";
import amenity_overlay from "../basemap/layers/amenity/overlay.js";
import boundary_line from "../basemap/layers/boundary/line.js";
import landuse_background from "../basemap/layers/landuse/background.js";
import landuse_overlay from "../basemap/layers/landuse/overlay.js";
import natural_background from "../basemap/layers/natural/background.js";
import natural_overlay from "../basemap/layers/natural/overlay.js";
import natural_tree from "../basemap/layers/natural/tree.js";
import natural_trunk from "../basemap/layers/natural/trunk.js";
import power_background from "../basemap/layers/power/background.js";
import power_tower from "../basemap/layers/power/tower.js";
import power_cable from "../basemap/layers/power/cable.js";
import leisure_background from "../basemap/layers/leisure/background.js";
import leisure_overlay from "../basemap/layers/leisure/overlay.js";
import railway_tunnel from "../basemap/layers/railway/tunnel.js";
import railway_line from "../basemap/layers/railway/line.js";

import highway_line from '../basemap/layers/highway/highway_line.js';
import highway_outline from '../basemap/layers/highway/highway_outline.js';
import highway_tunnel_line from '../basemap/layers/highway/tunnel_line.js';
import highway_tunnel_outline from '../basemap/layers/highway/tunnel_outline.js';
import highway_fill from '../basemap/layers/highway/highway_fill.js';
import highway_bridge_line from '../basemap/layers/highway/bridge_line.js';
import highway_bridge_outline from '../basemap/layers/highway/bridge_outline.js';
import highway_construction_line from "../basemap/layers/highway/construction_line.js";
import highway_label from '../basemap/layers/highway/highway_label.js';

import ocean_overlay from '../basemap/layers/ocean/overlay.js';
import route_line from "../basemap/layers/route/style.js"
import building_number from "../basemap/layers/building/number.js";
import building_fill from "../basemap/layers/building/fill.js";
import building_extrusion from "../basemap/layers/building/extrusion.js";
import man_made_fill from "../basemap/layers/man_made/man_made_fill.js";
import man_made_line from "../basemap/layers/man_made/man_made_line.js";
import man_made_label from "../basemap/layers/man_made/man_made_label.js";
import waterway_line from "../basemap/layers/waterway/line.js"
import waterway_area from "../basemap/layers/waterway/area.js"
import waterway_label from "../basemap/layers/waterway/label.js"
import icon from "../basemap/layers/point/icon.js";
import place from '../basemap/layers/point/place.js';
import country_label from '../basemap/layers/point/country_label.js';

import coastline from './layers/coastline/style.js';
// import landcover from './layers/landcover/style.js';

export default {
    "version": 8,
    "name": "OpenStreetMapVecto",
    "center": config.center,
    "zoom": config.zoom,
    "sources": {
        "baremaps": {
            "type": "vector",
            "url": `${config.host}/tiles.json`
        }
    },
    "sprite": `https://www.baremaps.com/sprites/osm/sprite`,
    "glyphs": "https://www.baremaps.com/fonts/{fontstack}/{range}.pbf",
    "layers": [
        background,
        power_background,
        aeroway_fill,
        landuse_background,
        leisure_background,
        amenity_background,
        natural_background,
        landuse_overlay,
        natural_overlay,
        amenity_overlay,
        leisure_overlay,
        coastline,
        // landcover,
        waterway_line,
        waterway_area,
        man_made_fill,
        man_made_line,
        man_made_label,
        amenity_fountain,
        highway_tunnel_outline,
        highway_tunnel_line,
        railway_tunnel,
        building_fill,
        building_number,
        highway_construction_line,
        highway_outline,
        highway_line,
        highway_fill,
        railway_line,
        highway_bridge_outline,
        highway_bridge_line,
        highway_label,
        aeroway_line,
        route_line,
        power_cable,
        power_tower,
        natural_tree,
        natural_trunk,
        boundary_line,
        waterway_label,
        icon,
        place,
        country_label,
    ],
};