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
 * The places a visitor goes: zoos, theme parks, campsites, picnic sites.
 *
 * One fill, the way every other area on this map is drawn. A zoo used to be the exception: two
 * layers drawing one boundary, an inner line offset from the outer one by a distance that grew
 * from nothing at zoom 13 to five pixels at 19, so the wall of the zoo crept inwards as the map
 * zoomed in and the two lines were a single band at the zoom the layer started at. The other four
 * kinds of visitor attraction, all of them in the tiles already, were drawn as nothing at all.
 */
export default asLayerObject({
    id: 'tourism_fill',
    type: 'fill',
    sourceLayer: 'tourism',
    sourceQueries: [
        {"minzoom": 13, "maxzoom": 20, "from": "osm_tourism"},
    ],
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ['geometry-type'], 'Polygon'],
    directives: withSortKeys([
        {
            filter: [
                'in',
                ['get', 'tourism'],
                ['literal', ['zoo', 'theme_park', 'attraction']],
            ],
            'fill-color': theme.tourismAttractionFillColor,
        },
        {
            filter: [
                'in',
                ['get', 'tourism'],
                ['literal', ['camp_site', 'caravan_site', 'picnic_site']],
            ],
            'fill-color': theme.tourismCampSiteFillColor,
        },
    ]),
});
