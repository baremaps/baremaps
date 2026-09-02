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

/**
 * The leisure features drawn as a line rather than filled.
 *
 * There is no geometry filter. A nature reserve is almost always mapped as a closed way, which
 * arrives as a polygon, and this layer used to ask for LineStrings only: the boundary of a reserve
 * was in every tile and drawn in none. A line layer paints the ring of a polygon, so dropping the
 * test is all it takes to draw the edge of a reserve without filling it.
 */
export default asLayerObject({
    'id': 'leisure_line',
    sourceLayer: 'leisure',
    'type': 'line',
    'layout': {
        'line-cap': 'round',
        'line-join': 'round',
    },
    directives: withSortKeys([
        {
            'filter': ['==', ['get', 'leisure'], 'nature_reserve'],
            'line-color': theme.leisureNatureReserveLineColor,
            'line-width': 2,
        },
        {
            filter: ['==', ['get', 'leisure'], 'track'],
            'line-color': theme.leisureTrackLineColor,
            'line-width-stops': theme.leisureTrackLineWidth,
        },
    ]),
});
