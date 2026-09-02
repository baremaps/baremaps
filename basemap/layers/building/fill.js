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

/**
 * A building takes its colour from the land it stands on.
 *
 * A town tells a reader what its districts are for long before any label is legible, and the thing
 * that says so is the buildings: sheds on an industrial estate, shops along a high street, farm
 * buildings among fields. The land under them already carries that in `landuse`, and the buildings
 * carry the shape a reader actually looks at, so the colour is put where it is read rather than
 * left on the ground beneath.
 *
 * The classes are few on purpose. Landuse has some thirty values and the ground draws each in a
 * hue of its own, but a building is small, seen in quantity, and read at a glance, and a palette
 * that fine turns a street into confetti. What a reader can be asked to tell apart at this size is
 * work, trade and farming, against the ordinary building that is neither. So the values are
 * gathered into three, and everything else, housing above all, is left at the colour a building
 * has by default rather than given a fourth.
 */
export default {
    id: 'building',
    type: 'fill',
    sourceLayer: 'building',
    sourceQueries: [
        {"minzoom": 13, "maxzoom": 20, "from": "osm_building"},
    ],
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': true,
        'fill-color': [
            'match', ['get', 'zoning'],
            ['commercial', 'retail'],
            theme.buildingCommercialFillColor,
            ['industrial', 'railway', 'garages', 'quarry', 'landfill', 'brownfield', 'construction'],
            theme.buildingIndustrialFillColor,
            ['farmland', 'farmyard', 'meadow', 'orchard', 'vineyard', 'allotments',
                'greenhouse_horticulture', 'plant_nursery'],
            theme.buildingAgriculturalFillColor,
            theme.buildingFillColor,
        ],
        'fill-outline-color': theme.buildingOutlineColor,
        'fill-opacity': [
            'interpolate',
            ['linear'],
            ['zoom'],
            13, 0,
            13.5, 1
        ]
    },
    filter: ['all',
        ['==', ['geometry-type'], 'Polygon'],
        ['!=', ['get', 'building'], 'no'],
        ['!=', ['get', 'building:part'], 'no']
    ],
}
