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
        filter: ['==', ['get', 'landuse'], 'military'],
        'fill-color': theme.landuseMilitaryOverlayFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'forest'],
        'fill-color': theme.landuseForestOverlayFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'grass'],
        'fill-color': theme.landuseGrassOverlayFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'greenhouse_horticulture'],
        'fill-color': theme.landuseGreenhouseHorticultureOverlayFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'orchard'],
        'fill-color': theme.landuseOrchardOverlayFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'meadow'],
        'fill-color': theme.landuseMeadowOverlayFillColor,
    },
];

export default asLayerObject(withSortKeys(directives), {
    id: 'landuse_overlay',
    type: 'fill',
    source: 'baremaps',
    'source-layer': 'landuse',
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ['geometry-type'], 'Polygon'],
});
