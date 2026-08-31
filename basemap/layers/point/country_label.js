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
import {asLayerObject, withSymbolSortKeys} from "../../utils/utils.js";

let directives = [
    {
        filter: [
            'all',
            ['==', ['get', 'place'], 'country']
        ],
        'text-size': 16,
        'text-color': theme.placeCountryTextColor,
        'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
    },
];

export default asLayerObject(withSymbolSortKeys(directives), {
    id: 'country_label',
    type: 'symbol',
    sourceLayer: 'point',
    'minzoom': 1,
    'maxzoom': 6,
    layout: {
        visibility: 'visible',
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
    },
    paint: {
        'text-halo-color': theme.placeCountryTextHaloColor,
        'text-halo-width': 1,
    },
});
