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

export default {
    id: 'man_made_pier_label',
    type: 'symbol',
    filter: ['all', ['==', ['get', 'man_made'], 'pier']],
    sourceLayer: 'man_made',
    layout: {
        'text-field': ['get', 'name'],
        'text-font': ['Noto Sans Regular'],
        'symbol-placement': 'line-center',
        'text-size': 11,
    },
    minzoom: 15,
    paint: {
        'text-color': theme.manMadePierTextColor,
        'text-halo-color': theme.manMadePierTextHaloColor,
        'text-halo-width': 1.2,
    },
}
