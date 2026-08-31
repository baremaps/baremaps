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

import background from "./layers/background/style.js";
import aerialway_line from "./layers/aerialway/line.js";
import aerialway_circle from "./layers/aerialway/circle.js";
import aeroway_line from "./layers/aeroway/line.js";
import aeroway_fill from "./layers/aeroway/fill.js";
import amenity_background from "./layers/amenity/background.js";
import amenity_fountain from "./layers/amenity/fountain.js";
import amenity_overlay from "./layers/amenity/overlay.js";
import attraction_line from "./layers/attraction/line.js";
import boundary_line from "./layers/boundary/line.js";
import barrier_line from "./layers/barrier/line.js";
import landuse_background from "./layers/landuse/background.js";
import landuse_overlay from "./layers/landuse/overlay.js";
import natural_background from "./layers/natural/background.js";
import natural_overlay from "./layers/natural/overlay.js";
import natural_line from "./layers/natural/line.js";
import natural_tree from "./layers/natural/tree.js";
import natural_trunk from "./layers/natural/trunk.js";
import power_background from "./layers/power/background.js";
import power_tower from "./layers/power/tower.js";
import power_cable from "./layers/power/cable.js";
import leisure_background from "./layers/leisure/background.js";
import leisure_line from "./layers/leisure/line.js";
import leisure_overlay from "./layers/leisure/overlay.js";
import railway_tunnel from "./layers/railway/tunnel.js";
import railway_line from "./layers/railway/line.js";

import highway_line from './layers/highway/highway_line.js';
import highway_outline from './layers/highway/highway_outline.js';
import highway_tunnel_line from './layers/highway/tunnel_line.js';
import highway_tunnel_outline from './layers/highway/tunnel_outline.js';
import highway_fill from './layers/highway/highway_fill.js';
import highway_bridge_line from './layers/highway/bridge_line.js';
import highway_bridge_outline from './layers/highway/bridge_outline.js';
import highway_construction_line from "./layers/highway/construction_line.js";
import highway_label from './layers/highway/highway_label.js';

import ocean_overlay from './layers/ocean/overlay.js';
import route_line from "./layers/route/style.js"
import building_number from "./layers/building/number.js";
import building_fill from "./layers/building/fill.js";
import building_extrusion from "./layers/building/extrusion.js";
import man_made_fill from "./layers/man_made/man_made_fill.js";
import man_made_line from "./layers/man_made/man_made_line.js";
import man_made_label from "./layers/man_made/man_made_label.js";
import waterway_line from "./layers/waterway/line.js"
import waterway_area from "./layers/waterway/area.js"
import waterway_label from "./layers/waterway/label.js"
import icon from "./layers/point/icon.js";
import place from './layers/point/place.js';
import country_label from './layers/point/country_label.js';

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
    "sprite": "https://www.baremaps.com/assets/icons/icons",
    "glyphs": "https://www.baremaps.com/assets/fonts/{fontstack}/{range}.pbf",
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
        ocean_overlay,
        natural_line,
        barrier_line,
        waterway_line,
        waterway_area,
        man_made_fill,
        man_made_line,
        man_made_label,
        amenity_fountain,
        railway_tunnel,
        highway_tunnel_outline,
        highway_tunnel_line,
        building_fill,
        building_number,
        highway_construction_line,
        highway_fill,
        highway_outline,
        highway_line,
        railway_line,
        attraction_line,
        highway_bridge_outline,
        highway_bridge_line,
        highway_label,
        aeroway_line,
        route_line,
        aerialway_line,
        aerialway_circle,
        power_cable,
        power_tower,
        natural_tree,
        natural_trunk,
        leisure_line,
        boundary_line,
        waterway_label,
        //building_extrusion,
        icon,
        place,
        country_label,
    ],
};