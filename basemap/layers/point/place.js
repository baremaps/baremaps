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

/**
 * The names of inhabited places.
 *
 * Each class is one size and one colour, held at every zoom. A label said the same thing at zoom 6
 * and at zoom 16, and the size it was drawn at was the only thing that changed; a city name that
 * grows to sixty pixels as the map zooms in is a city name that is in the way. Which places are
 * worth naming at a zoom is decided once, by the chain of views in `queries/point.sql`, and not a
 * second time by a size that interpolates down to nothing.
 *
 * The classes are ordered by how many people the name stands for, and drawn in five weights of
 * grey rather than five colours: they are the same kind of thing at different scales, and a hue
 * would say they were different kinds. Where two labels compete for the same space MapLibre keeps
 * the one with the lower `symbol-sort-key`, which is the more populous of the two.
 */
export default asLayerObject({
    id: 'point_place',
    type: 'symbol',
    sourceLayer: 'point',
    minzoom: 2,
    layout: {
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
    },
    paint: {
        'text-halo-color': theme.placeTextHaloColor,
        'text-halo-width': 1,
    },
    directives: withSymbolSortKeys([
        {
            filter: ['==', ['get', 'place'], 'city'],
            'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
            'text-color': theme.placeCityTextColor,
            'text-size': 15,
        },
        {
            filter: ['==', ['get', 'place'], 'town'],
            'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
            'text-color': theme.placeTownTextColor,
            'text-size': 12.5,
        },
        {
            filter: ['==', ['get', 'place'], 'village'],
            'symbol-sort-key': ["-", ["to-number", ['get', 'population'], 0]],
            'text-color': theme.placeVillageTextColor,
            'text-size': 11,
        },
        {
            // The parts of a city. They only reach the tiles where a city is large enough to have
            // them, so no zoom range is needed here to keep them out of a map of the countryside.
            filter: [
                'in',
                ['get', 'place'],
                ['literal', ['suburb', 'quarter', 'neighbourhood']],
            ],
            'text-color': theme.placeSuburbTextColor,
            'text-size': 10.5,
        },
        {
            // The smallest places that carry a name, and the named places that hold nobody.
            filter: [
                'in',
                ['get', 'place'],
                ['literal', ['hamlet', 'isolated_dwelling', 'locality']],
            ],
            'text-color': theme.placeLocalityTextColor,
            'text-size': 10,
        },
    ]),
});
