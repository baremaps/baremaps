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
import {withSortKeys, asLayerObject} from "../../utils/utils.js";
import theme from "../../theme.js";

export default asLayerObject({
    id: 'ocean_overlay',
    type: 'fill',
    sourceLayer: 'ocean',
    sourceQueries: [
        {"minzoom": 0, "maxzoom": 10, "from": "osm_ocean_simplified"},
        {"minzoom": 10, "maxzoom": 20, "from": "osm_ocean"},
    ],
    paint: {
        'fill-antialias': false,
    },
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'ocean'], 'water'],
            'fill-color': theme.oceanWaterFillColor,
            'fill-sort-key': 10,
        },
    ]),
});
