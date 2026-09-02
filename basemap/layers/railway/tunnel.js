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
 * The railways under the ground, in the same three weights as the ones above it and one colour
 * lighter. This layer carries the queries and the generalization for both.
 */
export default asLayerObject({
    'id': 'railway_tunnel',
    sourceLayer: 'railway',
    sourceQueries: [
        {"minzoom": 7, "maxzoom": 20, "from": "osm_railway"},
    ],
    generalize: {
        kind: 'lines',
        by: 'railway',
        values: [
            'light_rail',
            'monorail',
            'rail',
            'subway',
            'tram'
        ],
        // A main line is worth a pixel on a map of a country; a tramway is worth one on a map of
        // the city it runs in.
        minzoom: {
            'rail': 7,
            'light_rail': 11,
            'monorail': 11,
            'subway': 11,
            'tram': 11,
        },
        // A siding is a railway and a dry bed is a waterway; neither is worth generalizing.
        filter: ['!', ['has', 'service']],
    },
    'type': 'line',
    'layout': {
        'line-cap': 'round',
        'line-join': 'round',
    },
    'filter': ['all',
        ['==', ['geometry-type'], 'LineString'],
        ['==', ['get', 'tunnel'], 'yes'],
    ],
    directives: withSortKeys([
        {
            'filter': [
                'all',
                ['in', ['get', 'railway'],
                    ['literal', ['rail', 'narrow_gauge', 'preserved', 'funicular']]],
                ['!', ['has', 'service']],
            ],
            'line-color': theme.railwayTunnelColor,
            'line-width-stops': theme.railwayLineWidth,
        },
        {
            'filter': ['in', ['get', 'railway'],
                ['literal', ['subway', 'tram', 'light_rail', 'monorail']]],
            'line-color': theme.railwayTunnelColor,
            'line-width-stops': theme.railwayUrbanLineWidth,
        },
        {
            'filter': ['any',
                ['has', 'service'],
                ['in', ['get', 'railway'],
                    ['literal', ['turntable', 'construction', 'abandoned', 'disused',
                        'miniature']]],
            ],
            'line-color': theme.railwayTunnelColor,
            'line-width-stops': theme.railwayMinorLineWidth,
        },
    ]),
});
