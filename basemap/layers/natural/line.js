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
    id: 'natural_line',
    sourceLayer: 'natural',
    type: 'line',
    layout: {
        'line-cap': 'round',
        'line-join': 'round',
    },
    filter: [
        'all',
        ['==', ['geometry-type'], 'LineString'],
    ],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'natural'], 'cliff'],
            'line-color': theme.naturalCliffLineColor,
            'line-width-stops': theme.naturalCliffLineWidth,
        },
        {
            filter: ['==', ['get', 'natural'], 'tree_row'],
            'line-color': theme.naturalTreeRowLineColor,
            'line-width-stops': theme.naturalTreeRowLineWidth,
        },

    ]),
});
