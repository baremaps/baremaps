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
    id: 'highway_tunnel_outline',
    sourceLayer: 'highway',
    sourceQueries: [
        // Below 13 the generalized levels; at 13 the classes worth drawing when they stop being
        // generalized; above it, every highway.
        {"minzoom": 4, "maxzoom": 13, "from": "osm_highway"},
        {
            "minzoom": 13, "maxzoom": 14, "from": "osm_highway",
            "filter": ["in", ["get", "highway"], ["literal", ["motorway", "motorway_link", "primary", "primary_link", "residential", "secondary", "secondary_link", "tertiary", "tertiary_link", "trunk", "trunk_link", "unclassified"]]]
        },
        {"minzoom": 14, "maxzoom": 20, "from": "osm_highway"},
    ],
    generalize: {
        kind: 'lines',
        by: 'highway',
        values: [
            'motorway',
            'motorway_link',
            'primary',
            'primary_link',
            'residential',
            'secondary',
            'secondary_link',
            'tertiary',
            'tertiary_link',
            'trunk',
            'trunk_link',
            'unclassified'
        ],
        minzoom: {
            'motorway': 4,
            'motorway_link': 9,
            'primary': 6,
            'primary_link': 9,
            'residential': 11,
            'secondary': 9,
            'secondary_link': 9,
            'tertiary': 10,
            'tertiary_link': 10,
            'trunk': 6,
            'trunk_link': 9,
            'unclassified': 11
        },
    },
    type: 'line',
    layout: {
        'line-cap': 'square',
        'line-join': 'miter',
    },
    filter: [
        'all',
        ['==', ['geometry-type'], 'LineString'],
        ['==', ['get', 'tunnel'], 'yes'],
    ],
    directives: withSortKeys([
        {
            filter: [
                'any',
                ['==', ['get', 'highway'], 'motorway'],
                ['==', ['get', 'highway'], 'motorway_link'],
            ],
            'line-color': theme.highwayMotorwayTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayMotorwayLineWidth,
            'line-width': 1,
        },
        {
            filter: [
                'any',
                ['==', ['get', 'highway'], 'trunk'],
                ['==', ['get', 'highway'], 'trunk_link'],
            ],
            'line-color': theme.highwayTrunkTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayTrunkLineWidth,
            'line-width': 1,
        },
        {
            filter: [
                'any',
                ['==', ['get', 'highway'], 'primary'],
                ['==', ['get', 'highway'], 'primary_link'],
            ],
            'line-color': theme.highwayPrimaryTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayPrimaryLineWidth,
            'line-width': 1,
        },
        {
            filter: [
                'any',
                ['==', ['get', 'highway'], 'secondary'],
                ['==', ['get', 'highway'], 'secondary_link'],
            ],
            'line-color': theme.highwaySecondaryTunnelOutlineColor,
            'line-gap-width-stops': theme.highwaySecondaryLineWidth,
            'line-width': 1,
        },
        {
            filter: [
                'any',
                ['==', ['get', 'highway'], 'tertiary'],
                ['==', ['get', 'highway'], 'tertiary_link'],
            ],
            'line-color': theme.highwayTertiaryTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayTertiaryLineWidth,
            'line-width': 1,
        },
        {
            // A busway keeps the casing it has in the open. The tunnel and bridge colours are
            // named per road class and there is no busway among them, and a class with no casing
            // at all reads as a gap in the road rather than as a busway.
            filter: ['==', ['get', 'highway'], 'busway'],
            'line-color': theme.highwayBuswayOutlineColor,
            'line-gap-width-stops': theme.highwayBuswayLineWidth,
            'line-width': 1,
        },
        {
            filter: ['==', ['get', 'highway'], 'unclassified'],
            'line-color': theme.highwayUnclassifiedTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayUnclassifiedLineWidth,
            'line-width': 1,
        },
        {
            filter: ['==', ['get', 'highway'], 'residential'],
            'line-color': theme.highwayResidentialTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayResidentialLineWidth,
            'line-width': 1,
        },
        {
            filter: ['==', ['get', 'highway'], 'living_street'],
            'line-color': theme.highwayLivingStreetTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayLivingStreetLineWidth,
            'line-width': 1,
        },
        {
            filter: ['==', ['get', 'highway'], 'service'],
            'line-color': theme.highwayServiceTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayServiceLineWidth,
            'line-width': 1,
        },
        {
            filter: [
                'all',
                ['==', ['get', 'highway'], 'pedestrian'],
                ['!=', ['geometry-type'], 'Polygon'],
            ],
            'line-color': theme.highwayPedestrianTunnelOutlineColor,
            'line-gap-width-stops': theme.highwayPedestrianLineWidth,
            'line-width': 1,
        },
    ]),
});
