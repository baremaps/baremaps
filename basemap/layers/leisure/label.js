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
import {asLayerObject, withSymbolSortKeys} from "../../utils/utils.js";
import theme from "../../theme.js";

/**
 * The names of parks, reserves and the places sport is played.
 *
 * A green shape with no name is a green shape; the theme has held `leisureParkTextColor` and its
 * neighbours since before there was a layer to read them.
 */
export default asLayerObject({
    id: 'leisure_label',
    type: 'symbol',
    sourceLayer: 'leisure',
    minzoom: 13,
    filter: ['==', ['geometry-type'], 'Polygon'],
    layout: {
        visibility: 'visible',
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
        'text-size': 11,
        'text-max-width': 6,
    },
    paint: {
        'text-halo-color': theme.pointTextHaloColor,
        'text-halo-width': 1,
    },
    directives: withSymbolSortKeys([
        {
            filter: [
                'in',
                ['get', 'leisure'],
                ['literal', ['park', 'garden', 'nature_reserve', 'golf_course']],
            ],
            'text-color': theme.leisureParkTextColor,
        },
        {
            filter: ['in', ['get', 'leisure'], ['literal', ['stadium', 'sports_centre']]],
            'text-color': theme.leisureStadiumTextColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'pitch'],
            'text-color': theme.leisurePitchTextColor,
        },
    ]),
});
