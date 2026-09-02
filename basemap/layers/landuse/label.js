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
import {asLayerObject, withSymbolSortKeys} from "../../utils/utils.js";
import theme from "../../theme.js";

/**
 * The names of the areas a town is made of: the forest on its edge, the industrial estate, the
 * cemetery, the quarry.
 *
 * Each is lettered in a darker shade of its own fill, so that a name reads as belonging to the
 * area under it rather than as a label lying on top of one.
 */
export default asLayerObject({
    id: 'landuse_label',
    type: 'symbol',
    sourceLayer: 'landuse',
    minzoom: 13,
    filter: ['==', ['geometry-type'], 'Polygon'],
    layout: {
        visibility: 'visible',
        'text-font': ['Noto Sans Regular'],
        'text-field': ['get', 'name'],
        'text-size': 11,
        'text-max-width': 6,
    },
    paint: {
        'text-halo-color': theme.pointTextHaloColor,
        'text-halo-width': 1,
    },
    directives: withSymbolSortKeys([
        {
            filter: ['==', ['get', 'landuse'], 'forest'],
            'text-color': theme.landuseForestTextColor,
        },
        {
            filter: ['in', ['get', 'landuse'], ['literal', ['industrial', 'railway']]],
            'text-color': theme.landuseIndustrialTextColor,
        },
        {
            filter: ['in', ['get', 'landuse'], ['literal', ['quarry', 'landfill']]],
            'text-color': theme.landuseQuarryTextColor,
        },
        {
            filter: ['==', ['get', 'landuse'], 'cemetery'],
            'text-color': theme.buildingCemeteryTextColor,
        },
    ]),
});
