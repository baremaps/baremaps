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

/**
 * The map: a few properties of the map as a whole, and its layers in the order they are painted.
 *
 * Baremaps derives `style.json` and `tileset.json` from this. A layer says how it is drawn and,
 * for one layer per source layer, where its features come from; everything else is worked out.
 * The source each layer reads is always the same one, so no layer names it. The attributes the
 * tiles carry are read off the layers that use them, per zoom level, so adding a directive that
 * reads a new tag is all it takes to make the tiles carry it.
 *
 * The list is ordered, because paint order is global and this map interleaves its subjects: every
 * background is drawn before any overlay, and tunnels, buildings, roads and bridges stack in that
 * sequence whichever subject they belong to.
 */
import config from "./config.js";

import background from "./layers/background/style.js";
import power_background from "./layers/power/background.js";
import aeroway_fill from "./layers/aeroway/fill.js";
import landuse_background from "./layers/landuse/background.js";
import leisure_background from "./layers/leisure/background.js";
import amenity_background from "./layers/amenity/background.js";
import natural_background from "./layers/natural/background.js";
import landuse_overlay from "./layers/landuse/overlay.js";
import natural_overlay from "./layers/natural/overlay.js";
import amenity_overlay from "./layers/amenity/overlay.js";
import leisure_overlay from "./layers/leisure/overlay.js";
import ocean_overlay from "./layers/ocean/overlay.js";
import natural_line from "./layers/natural/line.js";
import barrier_line from "./layers/barrier/line.js";
import waterway_line from "./layers/waterway/line.js";
import waterway_area from "./layers/waterway/area.js";
import man_made_fill from "./layers/man_made/man_made_fill.js";
import man_made_line from "./layers/man_made/man_made_line.js";
import man_made_label from "./layers/man_made/man_made_label.js";
import amenity_fountain from "./layers/amenity/fountain.js";
import railway_tunnel from "./layers/railway/tunnel.js";
import highway_tunnel_outline from "./layers/highway/tunnel_outline.js";
import highway_tunnel_line from "./layers/highway/tunnel_line.js";
import building_fill from "./layers/building/fill.js";
import building_number from "./layers/building/number.js";
import highway_construction_line from "./layers/highway/construction_line.js";
import highway_fill from "./layers/highway/highway_fill.js";
import highway_outline from "./layers/highway/highway_outline.js";
import highway_line from "./layers/highway/highway_line.js";
import railway_line from "./layers/railway/line.js";
import attraction_line from "./layers/attraction/line.js";
import highway_bridge_outline from "./layers/highway/bridge_outline.js";
import highway_bridge_line from "./layers/highway/bridge_line.js";
import highway_label from "./layers/highway/highway_label.js";
import aeroway_line from "./layers/aeroway/line.js";
import route from "./layers/route/style.js";
import aerialway_line from "./layers/aerialway/line.js";
import aerialway_circle from "./layers/aerialway/circle.js";
import power_cable from "./layers/power/cable.js";
import power_tower from "./layers/power/tower.js";
import natural_tree from "./layers/natural/tree.js";
import natural_trunk from "./layers/natural/trunk.js";
import leisure_line from "./layers/leisure/line.js";
import tourism_style_zoo_fill from "./layers/tourism/style_zoo_fill.js";
import tourism_style_zoo_line from "./layers/tourism/style_zoo_line.js";
import boundary_line from "./layers/boundary/line.js";
import waterway_label from "./layers/waterway/label.js";
import point_icon from "./layers/point/icon.js";
import point_place from "./layers/point/place.js";
import point_country_label from "./layers/point/country_label.js";

export default {
    name: "OpenStreetMapVecto",
    center: config.center,
    zoom: config.zoom,
    bounds: config.bounds,
    attribution: '© <a href="https://www.openstreetmap.org/">OpenStreetMap</a>',
    sprite: "https://www.baremaps.com/assets/icons/icons",
    glyphs: "https://www.baremaps.com/assets/fonts/{fontstack}/{range}.pbf",
    tilejson: `${config.host}/tiles.json`,
    tiles: [`${config.host}/tiles/{z}/{x}/{y}.mvt`],
    database: config.database,
    minzoom: 0,
    maxzoom: 16,

    // What has to exist before the layers do: the extensions and functions the queries rely on,
    // and the tables the sources are read out of. Everything after this point is derived from the
    // layers themselves.
    schema: [
        "queries/initialize.sql",
        "queries/functions.sql",
        "layers/header/create.sql",
        "layers/node/create.sql",
        "layers/way/create.sql",
        "layers/relation/create.sql",
        "layers/member/create.sql",
        "layers/linestring/create.sql",
    ],

    // Bottom to top.
    layers: [
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
    route,
    aerialway_line,
    aerialway_circle,
    power_cable,
    power_tower,
    natural_tree,
    natural_trunk,
    leisure_line,
    tourism_style_zoo_fill,
    tourism_style_zoo_line,
    boundary_line,
    waterway_label,
    point_icon,
    point_place,
    point_country_label,
    ],
};
