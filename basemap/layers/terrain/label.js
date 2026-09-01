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
 * The elevation of the index contours.
 *
 * Contours without numbers say where the ground is steep and not how high it is, and reading a
 * height off them means counting lines from a summit. Only the index contours carry one, which is
 * what makes them worth drawing more heavily in the first place, and only from zoom 13, below which
 * the interval is coarse enough that the numbers crowd the lines they belong to.
 *
 * This is drawn among the labels rather than beside the contours, so that a place name or a river
 * wins the space where they compete: MapLibre resolves that collision in favour of whichever symbol
 * layer is painted later.
 */
export default {
    id: 'terrain_contour_label',
    type: 'symbol',
    source: 'terrain',
    sourceLayer: 'contour',
    minzoom: 13,
    filter: ['==', ['get', 'index'], 'yes'],
    layout: {
        visibility: 'visible',
        // One label at the middle of each contour rather than one every so many pixels along it.
        // A contour is cut at the edge of the tile that carries it, so the pieces are short and
        // uneven, and spacing labels along them leaves the short pieces unlabelled.
        'symbol-placement': 'line-center',
        'text-field': ['get', 'level'],
        'text-font': ['Noto Sans Regular'],
        'text-size': ['interpolate', ['linear'], ['zoom'], 13, 9, 16, 11],
        // Straight enough that the digits do not bend around the nose of a spur.
        'text-max-angle': 25,
        'text-padding': 8,
        'text-rotation-alignment': 'map',
    },
    paint: {
        'text-color': theme.terrainContourTextColor,
        'text-halo-color': theme.terrainContourTextHaloColor,
        'text-halo-width': 1.2,
    },
};
