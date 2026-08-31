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


export let directives = [
    {
        'filter': ['==', ['get', 'leisure'], 'nature_reserve'],
        'line-color': theme.leisureNatureReserveLineColor,
        'line-width': 2,
    },
    {
        filter: ['==', ['get', 'leisure'], 'track'],
        'line-color': theme.leisureTrackLineColor,
        'line-width-stops': theme.leisureTrackLineWidth,
    },
];

export default asLayerObject(withSortKeys(directives), {
    'id': 'leisure_line',
    sourceLayer: 'leisure',
    'type': 'line',
    'layout': {
        'visibility': 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
    'filter': ['==', ['geometry-type'], 'LineString'],
});
