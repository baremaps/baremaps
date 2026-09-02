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
import {asLayerObject, withSortKeys} from "../../utils/utils.js";

/**
 * The roads that are not roads yet.
 *
 * One grey line, whatever class is being built. A road under construction cannot be driven, so the
 * class it will belong to when it opens is not what the reader needs from it; that it is there and
 * closed is. Drawing it in the colour of the finished class said the opposite, and only the dash
 * pattern took it back.
 */
export default asLayerObject({
    id: 'highway_construction_line',
    sourceLayer: 'highway',
    type: 'line',
    layout: {
        visibility: 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
    filter: ['==', ['geometry-type'], 'LineString'],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'highway'], 'construction'],
            'line-color': theme.highwayDefaultConstructionLineColor,
            'line-width-stops': theme.highwayConstructionLineWidth,
        },
    ]),
});
