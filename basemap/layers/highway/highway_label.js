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
    id: 'highway_label',
    type: 'symbol',
    source: 'baremaps',
    'source-layer': 'highway',
    layout: {
        'symbol-placement': 'line',
        'text-anchor': 'center',
        'text-field': ['get', 'name'],
        'text-font': ['Noto Sans Regular'],
        'text-size': [
            'interpolate',
            ['linear'],
            ['zoom'],
            0, 0,
            10, 6,
            24, 24
        ],
        visibility: 'visible',
    },
    paint: {
        'text-color': theme.highwayLabelColor,
        'text-halo-color': theme.highwayLabelHaloColor,
        'text-halo-width': 1.2,
    },
}
