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
 * The relief, as six nested regions rather than as a shaded image.
 *
 * A raster hillshade carries its own colours and its own opacity, and cannot be told to suit a dark
 * map or a colour-vision theme. These are polygons, so the shading is six theme colours like any
 * other, and every derived theme gets a relief that matches the map it is drawn on.
 *
 * The regions nest, from the broadest and faintest to the smallest and strongest, and each is drawn
 * over the last. The colours are therefore what each level adds rather than what it ends up as,
 * which is why they are so faint on their own. The order the directives are written in is the order
 * they are painted, topmost first: the lit side covers the shaded one where they meet.
 */
export default asLayerObject({
    id: 'terrain_hillshade',
    type: 'fill',
    sourceLayer: 'hillshade',
    paint: {
        // The levels abut, and antialiasing every shared edge draws a seam along all of them.
        'fill-antialias': false,
    },
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'level'], '1'],
            'fill-color': theme.terrainHillshadeLit1FillColor,
        },
        {
            filter: ['==', ['get', 'level'], '2'],
            'fill-color': theme.terrainHillshadeLit2FillColor,
        },
        {
            filter: ['==', ['get', 'level'], '6'],
            'fill-color': theme.terrainHillshadeShade4FillColor,
        },
        {
            filter: ['==', ['get', 'level'], '5'],
            'fill-color': theme.terrainHillshadeShade3FillColor,
        },
        {
            filter: ['==', ['get', 'level'], '4'],
            'fill-color': theme.terrainHillshadeShade2FillColor,
        },
        {
            filter: ['==', ['get', 'level'], '3'],
            'fill-color': theme.terrainHillshadeShade1FillColor,
        },
    ]),
});
