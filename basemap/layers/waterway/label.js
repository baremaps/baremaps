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
    id: 'waterway_label',
    type: 'symbol',
    minzoom: 12,
    filter: ['==', ['get', 'waterway'], 'river'],
    sourceLayer: 'waterway',
    layout: {
        visibility: 'visible',
        'text-font': ['Noto Sans Italic'],
        'text-field': ['get', 'name'],
        'text-size': [
            'interpolate',
            ['exponential', 1.2],
            ['zoom'],
            12,
            9,
            15,
            12,
            18,
            11,
        ],
        'symbol-placement': 'line',
    },
    paint: {
        'text-color': theme.waterwayTextColor,
        'text-halo-color': theme.waterwayTextHaloColor,
        'text-halo-width': 1.2,
    },
}
