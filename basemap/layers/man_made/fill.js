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

import {asLayerObject} from "../../utils/utils.js";
import theme from "../../theme.js";

export default asLayerObject({
    id: 'man_made_bridge',
    type: 'fill',
    sourceLayer: 'man_made',
    sourceQueries: [
        {"minzoom": 14, "maxzoom": 20, "from": "osm_man_made"},
    ],
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ["geometry-type"], 'Polygon'],
    directives: [
        {
            filter: ['==', ['get', 'man_made'], 'bridge'],
            'fill-color': theme.manMadeBridgeFillColor,
        },
        {
            filter: ['==', ['get', 'man_made'], 'groyne'],
            'fill-color': theme.manMadeGroyneFillColor,
        },
        {
            filter: ['==', ['get', 'man_made'], 'pier'],
            'fill-color': theme.manMadePierFillColor,
        },
        {
            filter: ['==', ['get', 'man_made'], 'wastewater_plant'],
            'fill-color': theme.manMadeWasteWaterPlantFillColor,
        }
    ],
});
