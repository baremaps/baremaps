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
    id: 'natural_overlay',
    type: 'fill',
    sourceLayer: 'natural',
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ['geometry-type'], 'Polygon'],
    directives: withSortKeys([
        {
            filter: [
                'all',
                ['==', ['get', 'natural'], 'beach'],
                ['==', ['get', 'surface'], 'gravel']
            ],
            'fill-color': theme.naturalBeachGravelOverlayFillColor
        },
        {
            filter: ['==', ['get', 'natural'], 'beach'],
            'fill-color': theme.naturalBeachOverlayFillColor
        },
        {
            filter: ['==', ['get', 'natural'], 'sand'],
            'fill-color': theme.naturalSandOverlayFillColor
        },
        {
            // Every kind of water, in one colour. A lake used to be drawn by `natural` and a pond
            // by this layer, in the same blue: two rules, one result, and a lake that went under
            // the grass drawn between them.
            filter: ['==', ['get', 'natural'], 'water'],
            'fill-color': theme.naturalWaterFillColor
        },
        {
            filter: ['in', ['get', 'natural'], ['literal', ['wetland', 'mud']]],
            'fill-color': theme.naturalWetlandOverlayFillColor
        },
    ]),
});
