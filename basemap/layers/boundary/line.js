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

export default asLayerObject({
    id: 'boundary',
    type: 'line',
    sourceLayer: 'boundary',
    sourceQueries: [
        {"minzoom": 13, "maxzoom": 20, "from": "osm_boundary"},
    ],
    layout: {
        visibility: 'visible',
    },
    paint: {
        'line-dasharray': [4, 1, 1, 1],
    },
    filter: ['==', ["geometry-type"], 'LineString'],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'admin_level'], "1"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 3,
        },
        {
            filter: ['==', ['get', 'admin_level'], "2"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 3,
        },
        {
            filter: ['==', ['get', 'admin_level'], "3"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 2,
        },
        {
            filter: ['==', ['get', 'admin_level'], "4"],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 2,
        },
        {
            filter: ['has', 'boundary'],
            'line-color': theme.boundaryAdminLevelLineColor,
            'line-width': 1,
        },
    ]),
});
