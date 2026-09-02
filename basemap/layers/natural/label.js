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
 * The names of lakes, woods and glaciers.
 *
 * Nothing named an area before this: a map of Switzerland drew every lake and named none of them,
 * and the theme had carried the colours to do it with for as long as it had carried the colours
 * for the fills. The name of a body of water is set in italic, which is the one convention old
 * enough that a reader follows it without being told, and the same one `waterway_label` already
 * uses for rivers.
 *
 * They start at zoom 13 because that is where the generalized levels stop: below it neighbouring
 * areas of the same kind have been merged into one, and a merged wood is not the wood that carried
 * the name.
 */
export default asLayerObject({
    id: 'natural_label',
    type: 'symbol',
    sourceLayer: 'natural',
    minzoom: 13,
    filter: ['==', ['geometry-type'], 'Polygon'],
    layout: {
        // Italic throughout: physical geography is set in italic by a convention old enough
        // that a reader follows it without being told, and `waterway_label` already does.
        'text-font': ['Noto Sans Italic'],
        'text-field': ['get', 'name'],
        'text-size': 12,
        'text-max-width': 6,
    },
    paint: {
        'text-halo-color': theme.pointTextHaloColor,
        'text-halo-width': 1.5,
        'text-halo-blur': 0.5,
    },
    directives: withSymbolSortKeys([
        {
            filter: ['in', ['get', 'natural'], ['literal', ['water', 'bay', 'strait']]],
            'text-color': theme.waterwayTextColor,
        },
        {
            filter: ['in', ['get', 'natural'], ['literal', ['wood', 'scrub', 'heath']]],
            'text-color': theme.landuseForestTextColor,
        },
        {
            filter: ['==', ['get', 'natural'], 'glacier'],
            'text-color': theme.naturalGlacierTextColor,
        },
    ]),
});
