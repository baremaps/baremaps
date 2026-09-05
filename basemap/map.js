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
 * The map: how it presents itself, where its tiles come from, and its layers in the order they are
 * painted. Baremaps derives `style.json` and `tileset.json` from this.
 *
 * The document has three parts and every property belongs to exactly one of them. How the map
 * presents itself is the style specification's own vocabulary, spelled its way. Where its tiles
 * come from is `source`, which is that specification's vector source. What the tiles are built out
 * of is `database`, `schema` and `terrain`, which the browser never sees. Then the layers.
 *
 * A layer says how it is drawn and, for one layer per source layer, where its features come from;
 * everything else is worked out. Every layer reads the one source, so no layer names it. The
 * attributes the tiles carry are read off the layers that use them, per zoom level, so adding a
 * directive that reads a new tag is all it takes to make the tiles carry it. An attribute nothing
 * draws with leaves no such trace, and is the one thing a layer states outright, as `attributes`.
 *
 * A layer module lives at `layers/<topic>/<name>.js`, is imported under the two joined by an
 * underscore, and gives its layer that same name as its id, so a name can be read off a path and a
 * path off a name. The id is the handle everything outside this map holds the layer by, from
 * `setPaintProperty` to the `beforeId` someone inserts a layer of their own at, so it is fixed by
 * the module rather than chosen, and `validate.js` reports the two drifting apart. A module named
 * `style.js` is the only layer its topic has and takes the topic alone as its name.
 */
import config from "./config.js";

import background from "./layers/background/style.js";
import power_plant from "./layers/power/plant.js";
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
import terrain_hillshade from "./layers/terrain/hillshade.js";
import terrain_contour from "./layers/terrain/contour.js";
import terrain_contour_label from "./layers/terrain/contour_label.js";
import natural_line from "./layers/natural/line.js";
import barrier_line from "./layers/barrier/line.js";
import waterway_line from "./layers/waterway/line.js";
import waterway_area from "./layers/waterway/area.js";
import man_made_bridge from "./layers/man_made/bridge.js";
import man_made_pier_line from "./layers/man_made/pier_line.js";
import man_made_pier_label from "./layers/man_made/pier_label.js";
import amenity_fountain from "./layers/amenity/fountain.js";
import railway_tunnel from "./layers/railway/tunnel.js";
import highway_tunnel_outline from "./layers/highway/tunnel_outline.js";
import highway_tunnel_line from "./layers/highway/tunnel_line.js";
import building_fill from "./layers/building/fill.js";
import building_number from "./layers/building/number.js";
import highway_construction_line from "./layers/highway/construction_line.js";
import highway_pedestrian_area from "./layers/highway/pedestrian_area.js";
import highway_outline from "./layers/highway/outline.js";
import highway_line from "./layers/highway/line.js";
import railway_line from "./layers/railway/line.js";
import attraction_line from "./layers/attraction/line.js";
import highway_bridge_outline from "./layers/highway/bridge_outline.js";
import highway_bridge_line from "./layers/highway/bridge_line.js";
import highway_label from "./layers/highway/label.js";
import aeroway_line from "./layers/aeroway/line.js";
import route_ferry from "./layers/route/ferry.js";
import aerialway_line from "./layers/aerialway/line.js";
import aerialway_circle from "./layers/aerialway/circle.js";
import power_cable from "./layers/power/cable.js";
import power_tower from "./layers/power/tower.js";
import natural_tree from "./layers/natural/tree.js";
import natural_trunk from "./layers/natural/trunk.js";
import leisure_line from "./layers/leisure/line.js";
import tourism_fill from "./layers/tourism/fill.js";
import boundary_line from "./layers/boundary/line.js";
import landuse_label from "./layers/landuse/label.js";
import natural_label from "./layers/natural/label.js";
import leisure_label from "./layers/leisure/label.js";
import waterway_label from "./layers/waterway/label.js";
import point_icon from "./layers/point/icon.js";
import point_summit from "./layers/point/summit.js";
import point_place from "./layers/point/place.js";
import point_region from "./layers/point/region.js";
import point_country_label from "./layers/point/country_label.js";

export default {
    // How the map presents itself. These are the style specification's own properties, and they
    // are written out to the style unchanged.
    name: "OpenStreetMap Vecto",
    center: config.center,
    zoom: config.zoom,
    sprite: "https://www.baremaps.com/assets/icons/icons",
    glyphs: "https://www.baremaps.com/assets/fonts/{fontstack}/{range}.pbf",

    // The tiles the layers read, described the way the style specification describes a vector
    // source. There is one, and every layer reads it, so no layer names it. The relief and the
    // contours travel in it too, traced from the elevation declared below.
    source: {
        url: `${config.host}/tiles.json`,
        tiles: [`${config.host}/tiles/{z}/{x}/{y}.mvt`],
        bounds: config.bounds,
        minzoom: 0,
        maxzoom: 16,
        attribution: '© <a href="https://www.openstreetmap.org/">OpenStreetMap</a>',
    },

    // The elevation the relief and the contours are traced from. Not a second source: the tiles
    // above carry the traced shading and contours beside the layers the database answers with, so
    // a place arrives once, with its roads. Nothing about this reaches the database, and a map
    // that declares no terrain has none. Traced from zoom 4, below which relief is noise, and as
    // deep as the tiles go, there being no second pyramid left to stretch a shallower tile from.
    terrain: {
        dem: config.dem,
        demMaxzoom: config.demMaxzoom,
        minzoom: 4,
        attribution: '\u00a9 <a href="https://mapterhorn.com/">Mapterhorn</a>',
    },

    // What the tiles are built out of: the database the queries are run against, and the sql that
    // builds what the layers read, in the order it has to run. The extensions and functions first,
    // then the tables an import writes into, then the views the layers name.
    database: config.database,
    schema: [
        "queries/initialize.sql",
        "queries/functions.sql",
        "queries/header.sql",
        "queries/node.sql",
        "queries/way.sql",
        "queries/relation.sql",
        "queries/member.sql",
        "queries/linestring.sql",
        "queries/views.sql",
        "queries/point.sql",
    ],

    // The layers, bottom to top. The list is ordered, because paint order is global and this map
    // interleaves its subjects: every background is drawn before any overlay, and tunnels,
    // buildings, roads and bridges stack in that sequence whichever subject they belong to.
    layers: [
        background,
        power_plant,
        aeroway_fill,
        landuse_background,
        leisure_background,
        amenity_background,
        natural_background,
        landuse_overlay,
        natural_overlay,
        amenity_overlay,
        leisure_overlay,
        tourism_fill,
        // The relief and the contours shade the land cover drawn above, and are covered in turn by
        // the water and by everything linear: a road is not shaded and a lake has no contours.
        terrain_hillshade,
        terrain_contour,
        ocean_overlay,
        natural_line,
        barrier_line,
        waterway_line,
        waterway_area,
        man_made_bridge,
        man_made_pier_line,
        man_made_pier_label,
        amenity_fountain,
        railway_tunnel,
        highway_tunnel_outline,
        highway_tunnel_line,
        building_fill,
        building_number,
        highway_construction_line,
        highway_pedestrian_area,
        highway_outline,
        highway_line,
        railway_line,
        attraction_line,
        highway_bridge_outline,
        highway_bridge_line,
        highway_label,
        aeroway_line,
        route_ferry,
        aerialway_line,
        aerialway_circle,
        power_cable,
        power_tower,
        natural_tree,
        natural_trunk,
        leisure_line,
        boundary_line,
        // Among the labels, and first, so that a place name or a river wins where they compete.
        terrain_contour_label,
        landuse_label,
        natural_label,
        leisure_label,
        waterway_label,
        point_icon,
        point_summit,
        point_place,
        point_region,
        point_country_label,
    ],
};
