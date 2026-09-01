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
    id: 'leisure',
    type: 'fill',
    sourceLayer: 'leisure',
    sourceQueries: [
        {"minzoom": 1, "maxzoom": 20, "from": "osm_leisure", "filter": ["has", "leisure"]},
    ],
    generalize: {
        by: 'leisure',
        values: [
            'garden',
            'golf_course',
            'marina',
            'nature_reserve',
            'park',
            'pitch',
            'sport_center',
            'stadium',
            'swimming_pool',
            'track'
        ],
    },
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ["geometry-type"], 'Polygon'],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'leisure'], 'golf_course'],
            'fill-color': theme.leisureGolfCourseBackgroundFillColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'sports_centre'],
            'fill-color': theme.leisureSportsCentreBackgroundFillColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'garden'],
            'fill-color': theme.leisureGardenBackgroundFillColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'park'],
            'fill-color': theme.leisureParkBackgroundFillColor,
        },
    ]),
});
