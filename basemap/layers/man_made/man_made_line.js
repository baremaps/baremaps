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

let directives = [
    {
        filter: [
            'all',
            ['==', ["geometry-type"], 'LineString'],
            ['==', ['get', 'man_made'], 'pier'],
        ],
        'line-color': theme.manMadePierLineColor,
        'line-width-stops': theme.manMadePierLineWidth,
    }
];

export default asLayerObject(withSortKeys(directives), {
    id: 'man_made_pier_line',
    type: 'line',
    sourceLayer: 'man_made',
    layout: {
        visibility: 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
    minzoom: 12,
});
