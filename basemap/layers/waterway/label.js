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
        'text-font': ['Noto Sans Italic'],
        'text-field': ['get', 'name'],
        'text-size': 11,
        'symbol-placement': 'line',
        // A river bends more than a road does, so a name that will not sit along it is better
        // dropped than wrapped around the meander the default angle allows.
        'text-max-angle': 30,
    },
    paint: {
        'text-color': theme.waterwayTextColor,
        'text-halo-color': theme.waterwayTextHaloColor,
        'text-halo-width': 1.2,
    },
}
