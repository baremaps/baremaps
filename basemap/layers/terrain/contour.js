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

/**
 * The elevation contours.
 *
 * Every fifth contour is drawn more heavily, so that the relief can be read without counting lines.
 * Which contour that is comes from the tile rather than from a filter here, because the interval
 * between contours changes with the zoom level and a filter on the elevation would have to change
 * with it.
 *
 * They start at zoom 12. Below that the interval is five hundred metres or more, which says
 * nothing the shading has not already said and says it as a line across every hillside.
 */
export default asLayerObject({
    id: 'terrain_contour',
    type: 'line',
    sourceLayer: 'contour',
    minzoom: 12,
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'index'], 'yes'],
            'line-color': theme.terrainContourIndexLineColor,
            'line-width-stops': [12, 0.9, 16, 1.8],
        },
        {
            filter: ['!', ['has', 'index']],
            'line-color': theme.terrainContourLineColor,
            'line-width-stops': [13, 0.7, 16, 1.1],
        },
    ]),
});
