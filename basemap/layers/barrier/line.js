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
    id: 'barrier_line',
    sourceLayer: 'barrier',
    sourceQueries: [
        {"minzoom": 14, "maxzoom": 20, "from": "osm_barrier"},
    ],
    type: 'line',
    layout: {
        visibility: 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
    filter: [
        'all',
        ['==', ['geometry-type'], 'LineString'],
    ],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'barrier'], 'hedge'],
            'line-color': theme.barrierHedgeLineColor,
            'line-width-stops': theme.barrierHedgeLineWidth,
        },
        {
            filter: ['==', ['get', 'barrier'], 'wall'],
            'line-color': theme.barrierWallLineColor,
            'line-width-stops': theme.barrierWallLineWidth,
        },
        {
            filter: ['==', ['get', 'barrier'], 'fence'],
            'line-color': theme.barrierFenceLineColor,
            'line-width-stops': theme.barrierFenceLineWidth,
        },
        {
            filter: ['==', ['get', 'barrier'], 'city_wall'],
            'line-color': theme.barrierCityWallLineColor,
            'line-width-stops': theme.barrierCityWallLineWidth,
        },

    ]),
});
