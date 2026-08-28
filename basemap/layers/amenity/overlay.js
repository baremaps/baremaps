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
            ['!=', ['get', 'layer'], '-1'],
            ['any',
                ['==', ['get', 'amenity'], 'bicycle_parking'],
                ['==', ['get', 'amenity'], 'motorcycle_parking'],
                ['==', ['get', 'amenity'], 'parking'],
            ],
        ],
        'fill-color': theme.amenityParkingOverlayFillColor,
    },
    {
        filter: [
            'all',
            ['!=', ['get', 'layer'], '-1'],
            ['==', ['get', 'amenity'], 'parking_space'],
        ],
        'fill-color': theme.amenityParkingOverlayFillColor,
        'fill-outline-color': theme.amenityParkingOverlayOutlineColor,
    },
];

export default asLayerObject(withSortKeys(directives), {
    id: 'amenity_overlay',
    type: 'fill',
    source: 'baremaps',
    'source-layer': 'amenity',
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': true,
    },
    filter: ['==', ["geometry-type"], 'Polygon'],
});
