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
import {asLayerObject, withSortKeys} from "../../utils/utils.js";
import theme from "../../theme.js";

/**
 * The administrative boundaries.
 *
 * A border is one of the few things a map is read for at every zoom, so this is generalized down
 * to zoom 2 rather than starting where the untouched ways become affordable. The levels below 13
 * carry `admin_level` and nothing else, which is all the directives ask for.
 *
 * Only `boundary=administrative` is drawn. The same key also tags postal codes, protected areas
 * and political districts, and drawing them all in the one colour turns a country border into one
 * line among four that mean different things.
 *
 * The line is solid and its width does not change with the zoom level. A border has no width on
 * the ground, so any width is a convention, and one that holds still is one less thing moving when
 * the map does. Three widths and one colour say which level a border belongs to; a dash pattern
 * would say the same thing again, and at a width of one pixel it says it as a dotted smear.
 */
export default asLayerObject({
    id: 'boundary_line',
    type: 'line',
    sourceLayer: 'boundary',
    sourceQueries: [
        {"minzoom": 2, "maxzoom": 13, "from": "osm_boundary"},
        {
            "minzoom": 13, "maxzoom": 20, "from": "osm_boundary",
            "filter": ["==", ["get", "boundary"], "administrative"]
        },
    ],
    generalize: {
        kind: 'lines',
        by: 'admin_level',
        values: ['2', '4', '6'],
        // A country border is worth a pixel on a world map, a district border is not.
        minzoom: {'2': 2, '4': 5, '6': 9},
        filter: ['==', ['get', 'boundary'], 'administrative'],
    },
    layout: {
        'line-cap': 'round',
        'line-join': 'round',
    },
    filter: ['==', ["geometry-type"], 'LineString'],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'admin_level'], "2"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 1.6,
        },
        {
            filter: ['==', ['get', 'admin_level'], "4"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 1,
        },
        {
            filter: ['in', ['get', 'admin_level'], ['literal', ["5", "6"]]],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 0.6,
        },
    ]),
});
