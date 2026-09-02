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
import theme from "../../theme.js";

/**
 * The summits, with the height they stand at.
 *
 * `queries/point.sql` has carried peaks and volcanoes down to zoom 10 all along, but the layer
 * that drew them started at 14, so between the two they were paid for in every tile and drawn in
 * none. On a map that shades its relief and traces its contours, the summit is the one label that
 * says what the shading is of.
 *
 * The elevation is part of the name because it is what a peak is looked up for, and it is written
 * on its own line so that the name still reads as a name. A summit tagged without one is drawn
 * with its name alone rather than dropped.
 */
export default {
    id: 'point_summit',
    type: 'symbol',
    sourceLayer: 'point',
    minzoom: 12,
    // A summit with no name is a triangle that says nothing, and `concat` cannot build a label
    // out of a name that is not there.
    filter: [
        'all',
        ['in', ['get', 'natural'], ['literal', ['peak', 'volcano']]],
        ['has', 'name'],
    ],
    layout: {
        'icon-image': 'peak',
        'icon-anchor': 'bottom',
        'text-field': [
            'case',
            ['has', 'ele'], ['concat', ['get', 'name'], '\n', ['get', 'ele']],
            ['get', 'name'],
        ],
        'text-font': ['Noto Sans Regular'],
        'text-size': 12,
        'text-anchor': 'top',
        // The icon and the name are placed together or not at all. Left optional, MapLibre drops
        // the name of a crowded summit and keeps its triangle, and a range comes out as a field of
        // triangles that name nothing.
        'text-optional': false,
        'text-max-width': 6,
        'text-padding': 4,
        // The highest summit wins the space. Without this MapLibre keeps whichever peak the tile
        // happens to list first, and a range comes out labelled by its foothills.
        'symbol-sort-key': ['-', ['to-number', ['get', 'ele'], 0]],
    },
    paint: {
        'icon-color': theme.naturalPeakIconColor,
        'icon-halo-color': theme.pointIconHaloColor,
        'icon-halo-width': 1,
        'text-color': theme.naturalPeakTextColor,
        'text-halo-color': theme.pointTextHaloColor,
        'text-halo-width': 1.5,
        'text-halo-blur': 0.5,
    },
};
