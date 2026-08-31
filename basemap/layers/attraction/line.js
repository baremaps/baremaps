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
        filter: ['==', ['get', 'attraction'], "water_slide"],
        'line-color': theme.attractionWaterSlideLineColor,
        'line-width-stops': theme.attractionWaterSlideLineWidth,
    },

];

export default asLayerObject(withSortKeys(directives), {
    id: 'attraction_line',
    type: 'line',
    sourceLayer: 'attraction',
    sourceQueries: [
        {"minzoom": 13, "maxzoom": 20, "from": "osm_attraction"},
    ],
    sourceSchema: 'layers/attraction/create.sql',
    layout: {
        visibility: 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
});
