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
];

export default asLayerObject(withSortKeys(directives), {
    id: 'leisure',
    type: 'fill',
    source: 'baremaps',
    'source-layer': 'leisure',
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ["geometry-type"], 'Polygon'],
});
