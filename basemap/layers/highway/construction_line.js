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


let directives = [
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['!', ['has', 'construction']],
        ],
        'line-color': theme.highwayDefaultConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['any',
                ['==', ['get', 'construction'], 'motorway'],
                ['==', ['get', 'construction'], 'motorway_link'],
            ]
        ],
        'line-color': theme.highwayMotorwayConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['any',
                ['==', ['get', 'construction'], 'trunk'],
                ['==', ['get', 'construction'], 'trunk_link'],
            ]
        ],
        'line-color': theme.highwayTrunkConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['any',
                ['==', ['get', 'construction'], 'primary'],
                ['==', ['get', 'construction'], 'primary_link'],
            ]
        ],
        'line-color': theme.highwayPrimaryConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['any',
                ['==', ['get', 'construction'], 'secondary'],
                ['==', ['get', 'construction'], 'secondary_link'],
            ]
        ],
        'line-color': theme.highwaySecondaryConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['any',
                ['==', ['get', 'construction'], 'tertiary'],
                ['==', ['get', 'construction'], 'tertiary_link'],
            ]
        ],
        'line-color': theme.highwayTertiaryConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['==', ['get', 'construction'], 'unclassified'],
        ],
        'line-color': theme.highwayUnclassifiedConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['==', ['get', 'construction'], 'residential'],
        ],
        'line-color': theme.highwayResidentialConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['==', ['get', 'construction'], 'living_street'],
        ],
        'line-color': theme.highwayLivingStreetConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['==', ['get', 'construction'], 'service'],
        ],
        'line-color': theme.highwayServiceConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'construction'],
            ['==', ['get', 'construction'], 'raceway'],
        ],
        'line-color': theme.highwayRacewayConstructionLineColor,
        'line-width-stops': theme.highwayConstructionLineWidth,
    },
]

export default asLayerObject(withSortKeys(directives), {
    id: 'highway_construction_line',
    source: 'baremaps',
    'source-layer': 'highway',
    type: 'line',
    layout: {
        visibility: 'visible',
        'line-cap': 'butt',
        'line-join': 'round',
    },
    paint: {
        'line-dasharray': [1, 1],
    },
    filter: ['==', ['geometry-type'], 'LineString'],
});
