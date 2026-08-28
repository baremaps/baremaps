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

let directives = [
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'pedestrian'],
            ['!=', ['get', 'area'], 'yes'],
        ],
        'line-color': theme.highwayPedestrianLineColor,
        'line-width-stops': theme.highwayPedestrianLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'bridleway'],
        'line-color': theme.highwayDashBridlewayLineColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'busway'],
        'line-color': theme.highwayBuswayDashColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'cycleway'],
            [
                'all',
                ['==', ['get', 'highway'], 'path'],
                ['==', ['get', 'bicycle'], 'designated'],
            ],
        ],
        'line-color': theme.highwayCyclewayDashColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
    {
        filter: [
            'any',
            [
                'in',
                ['get', 'highway'],
                ['literal', ['sidewalk', 'crossing', 'steps']],
            ],
            [
                'all',
                ['==', ['get', 'highway'], 'footway'],
                ['!=', ['get', 'access'], 'private'],
            ],
            [
                'all',
                ['==', ['get', 'highway'], 'path'],
                ['!=', ['get', 'bicycle'], 'designated'],
            ],
        ],
        'line-color': theme.highwayHighwayDashColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
    {
        filter: ['all', ['==', ['get', 'highway'], 'track']],
        'line-color': theme.highwayTrackDashColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'motorway'],
            ['==', ['get', 'highway'], 'motorway_link'],
        ],
        'line-color': theme.highwayMotorwayTunnelLineColor,
        'line-width-stops': theme.highwayMotorwayLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'trunk'],
            ['==', ['get', 'highway'], 'trunk_link'],
        ],
        'line-color': theme.highwayTrunkTunnelLineColor,
        'line-width-stops': theme.highwayTrunkLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'primary'],
            ['==', ['get', 'highway'], 'primary_link'],
        ],
        'line-color': theme.highwayPrimaryTunnelLineColor,
        'line-width-stops': theme.highwayPrimaryLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'secondary'],
            ['==', ['get', 'highway'], 'secondary_link'],
        ],
        'line-color': theme.highwaySecondaryTunnelLineColor,
        'line-width-stops': theme.highwaySecondaryLineWidth,
    },
    {
        filter: [
            'any',
            ['==', ['get', 'highway'], 'tertiary'],
            ['==', ['get', 'highway'], 'tertiary_link'],
        ],
        'line-color': theme.highwayTertiaryTunnelLineColor,
        'line-width-stops': theme.highwayTertiaryLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'unclassified'],
        'line-color': theme.highwayUnclassifiedTunnelLineColor,
        'line-width-stops': theme.highwayUnclassifiedLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'residential'],
        'line-color': theme.highwayResidentialTunnelLineColor,
        'line-width-stops': theme.highwayResidentialLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'living_street'],
        'line-color': theme.highwayLivingStreetTunnelLineColor,
        'line-width-stops': theme.highwayLivingStreetLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'service'],
        'line-color': theme.highwayServiceTunnelLineColor,
        'line-width-stops': theme.highwayServiceLineWidth,
    },
    {
        filter: [
            'all',
            ['==', ['get', 'highway'], 'pedestrian'],
            ['!=', ['geometry-type'], 'Polygon'],
        ],
        'line-color': theme.highwayPedestrianTunnelLineColor,
        'line-width-stops': theme.highwayPedestrianLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'raceway'],
        'line-color': theme.highwayRacewayTunnelLineColor,
        'line-width-stops': theme.highwayRacewayLineWidth,
    },
    {
        filter: ['==', ['get', 'highway'], 'track'],
        'line-color': theme.highwayTrackTunnelLineColor,
        'line-width-stops': theme.highwayMinorLineWidth,
    },
];

export default asLayerObject(withSortKeys(directives), {
    id: 'tunnel_line',
    source: 'baremaps',
    'source-layer': 'highway',
    type: 'line',
    layout: {
        visibility: 'visible',
        'line-cap': 'square',
        'line-join': 'miter',
    },
    filter: [
        'all',
        ['==', ['geometry-type'], 'LineString'],
        ['==', ['get', 'tunnel'], 'yes'],
    ],
});
