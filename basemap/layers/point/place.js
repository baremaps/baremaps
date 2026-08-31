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
        'filter': [
            'all',
            ['==', ['get', 'place'], 'city'],
            ['==', ['get', 'capital'], 'yes'],
        ],
        'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
        'label-color': theme.placeIconColor,
        'text-size-stops': [
            0, 0,
            10, 18,
            24, 72
        ],
    },
    {
        'filter': [
            'all',
            ['==', ['get', 'place'], 'city'],
            ['!=', ['get', 'capital'], 'yes'],
        ],
        'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
        'label-color': [theme.placeCityCapitalTextColorOne, theme.placeCityCapitalTextColorTwo],
        'text-size-stops': [
            0, 0,
            10, 16,
            24, 64
        ],
    },
    {
        'filter': ['==', ['get', 'place'], 'town'],
        'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
        'label-color': [theme.placeTownTextColorOne, theme.placeTownTextColorTwo],
        'text-size-stops': [
            0, 0,
            10, 14,
            24, 56
        ],
    },
    {
        filter: ['==', ['get', 'place'], 'village'],
        'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
        'label-color': theme.placeVillageTextColor,
        'text-size-stops': [
            0, 0,
            10, 10,
            24, 40
        ],
    },
    {
        filter: ['==', ['get', 'place'], 'locality'],
        'label-color': theme.placeLocalityTextColor,
        'text-size-stops': [
            0, 0,
            10, 8,
            24, 32
        ],
    },
    // {
    //     filter: [
    //         'in',
    //         ['get', 'place'],
    //         [
    //             'literal', [
    //                 'neighbourhood',
    //                 'quarter',
    //                 'hamlet',
    //                 'isolated_dwelling',
    //                 'islet'
    //             ]
    //         ]
    //     ],
    //     'text-size': 11,
    //     'text-color': theme.placeTextColor,
    // },

];

export default asLayerObject(withSymbolSortKeys(directives), {
    id: 'point_label',
    type: 'symbol',
    sourceLayer: 'point',
    'minzoom': 2,
    'maxzoom': 24,
    layout: {
        visibility: 'visible',
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
        'icon-size': 1,
        'text-optional': true,
    },
    paint: {
        'text-halo-color': theme.placeTextHaloColor,
        'text-halo-width': 1,
    },
});
