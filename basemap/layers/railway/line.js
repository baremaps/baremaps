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
 * The railways, in three weights.
 *
 * OpenStreetMap distinguishes fourteen kinds of track and this draws three: the line trains run
 * on, the line a city runs on, and the line nothing runs on. The other eleven were drawn in eight
 * greys and eight widths that differed by fractions of a pixel, which is a distinction the eye
 * cannot make and the legend cannot explain.
 *
 * The greys are lighter than they were. A railway used to be the darkest line on the map at every
 * zoom, so a valley read as a railway with a motorway beside it rather than the other way round.
 */
export default asLayerObject({
    'id': 'railway_line',
    sourceLayer: 'railway',
    'type': 'line',
    'layout': {
        'visibility': 'visible',
        'line-cap': 'round',
        'line-join': 'round',
    },
    'filter': ['all',
        ['==', ['geometry-type'], 'LineString'],
        ['!=', ['get', 'tunnel'], 'yes'],
    ],
    directives: withSortKeys([
        {
            'filter': [
                'all',
                ['in', ['get', 'railway'],
                    ['literal', ['rail', 'narrow_gauge', 'preserved', 'funicular']]],
                ['!', ['has', 'service']],
            ],
            'line-color': theme.railwayLineColor,
            'line-width-stops': theme.railwayLineWidth,
        },
        {
            'filter': ['in', ['get', 'railway'],
                ['literal', ['subway', 'tram', 'light_rail', 'monorail']]],
            'line-color': theme.railwayUrbanLineColor,
            'line-width-stops': theme.railwayUrbanLineWidth,
        },
        {
            // A siding, a turntable, and everything no longer in use: present, and not part of the
            // network.
            'filter': ['any',
                ['has', 'service'],
                ['in', ['get', 'railway'],
                    ['literal', ['turntable', 'construction', 'abandoned', 'disused',
                        'miniature']]],
            ],
            'line-color': theme.railwayMinorLineColor,
            'line-width-stops': theme.railwayMinorLineWidth,
        },
    ]),
});
