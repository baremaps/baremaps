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
 * The names of the divisions of a country: states, cantons, regions, provinces.
 *
 * `queries/point.sql` has carried them since zoom 1 and nothing drew them, so a map showing the
 * borders of a country's states showed none of their names. They are lettered in the same purple
 * as the boundaries, because a border and the name of what it encloses are one thing said twice.
 *
 * They stop at zoom 11, where `queries/point.sql` stops carrying them, rather than reappearing on
 * a map of a city: the name of a canton over a street corner locates nothing.
 */
export default {
    id: 'point_region',
    type: 'symbol',
    sourceLayer: 'point',
    minzoom: 4,
    maxzoom: 11,
    filter: ['in', ['get', 'place'], ['literal', ['state', 'region', 'province']]],
    layout: {
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
        'text-size': 13,
        'text-letter-spacing': 0.08,
        'symbol-sort-key': ['-', ['to-number', ['get', 'rank'], 0]],
    },
    paint: {
        'text-color': theme.placeRegionTextColor,
        'text-halo-color': theme.placeCountryTextHaloColor,
        'text-halo-width': 1.5,
        'text-halo-blur': 0.5,
    },
};
