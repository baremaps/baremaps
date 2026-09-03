/**
 Licensed under the Apache License, Version 2.0
 (the 'License'); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an 'AS IS' BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
import {asLayerObject, withSymbolSortKeys} from '../../utils/utils.js';
import theme from '../../theme.js';

/**
 * What a class of point is worth when two of them want the same piece of the screen.
 *
 * A reader looks at a city centre for the station, the hospital, the town hall and the church
 * before they look at it for the nearest of three hundred shops, and the ones they look for are
 * the ones there are fewest of. So the bands run from what a stranger navigates by down to what
 * is worth an icon only where there is room for one, and `withSymbolSortKey` turns a band and the
 * position within it into the key MapLibre reads.
 *
 * The bands are coarse on purpose. They rank a class against the classes of other subjects, which
 * is the question the list the directives are written in cannot answer; ranking the classes of one
 * subject against each other is what their order inside a band already does.
 */
/**
 * The zoom each band arrives at.
 *
 * Two hundred and fifty six classes used to land on the map in one step at 16, and a city centre
 * came out as a field of icons: three tiles over the old town of Zurich offer eleven hundred and
 * eighty two of them, where a tile has room to draw perhaps thirty. Which thirty a reader saw was
 * settled by the collision grid rather than by anything anyone chose.
 *
 * So they arrive over three zooms, the way the road names do in `highway/label.js`, and for the
 * same reason: what a reader navigates by first, then what they are usually looking for, then the
 * rest. Five hundred and seventy seven of those eleven hundred are shops and places to eat, and
 * four hundred and eighty nine are street furniture -- benches, bins, bollards, gates, vending
 * machines -- of which thirty six carry a name. The rest is the hundred and sixteen a stranger
 * navigates by, which is about what a tile can hold.
 *
 * Each zoom quarters the ground a tile covers, so a band held back one zoom is drawn against four
 * times the room: the shops arrive at 17 as roughly a hundred and forty to a tile, and the street
 * furniture at 18 as thirty.
 *
 * A few classes arrive early despite this, and are meant to. What admits a feature to a zoom is a
 * lookup gathered per attribute, so a class sharing an attribute value with an earlier band comes
 * in with it: a street-side parking bay is admitted at 16 along with the car parks, because both
 * are `amenity=parking`. The chain then answers with the narrower directive, so it is drawn as the
 * quiet parking mark it should be rather than as a car park, and there are two of them in a tile
 * over the old town. Separating them would mean asking each class its own question, which is the
 * two hundred and fifty six comparisons the gathering exists to avoid.
 */
const ARRIVAL = {
    interchange: 16,
    emergency: 16,
    civic: 16,
    landmark: 16,
    transport: 16,
    commerce: 17,
    detail: 18,
};

const PRIORITY = {
    // Where a journey changes mode. Few, named, and the thing a route is built out of.
    interchange: 0,
    // What a reader looks for when something has gone wrong.
    emergency: 1,
    // The institutions a stranger asks for by name.
    civic: 2,
    // What a place is known for, and what it is recognised by from across a square.
    landmark: 3,
    // The rest of getting about: stops, ranks, car parks, fuel. Useful, and repetitive.
    transport: 4,
    // Shops, food and lodging. What most of the points are, and mostly interchangeable.
    commerce: 5,
    // Street furniture and equipment, down to the bins and the bollards.
    detail: 6,
};

/**
 * Gives each directive the zoom its band arrives at, which `asFilterProperty` gathers into a step.
 *
 * Read off the priority rather than written on each directive: the band already says what a class
 * is worth, and when it is drawn follows from that. A class that wants a zoom of its own says so
 * itself and keeps it.
 */
const BANDS = Object.fromEntries(Object.entries(PRIORITY).map(([band, value]) => [value, band]));

function arriving(directives) {
    return directives.map((directive) => directive['minzoom'] !== undefined ? directive : {
        ...directive,
        minzoom: ARRIVAL[BANDS[directive['priority']]],
    });
}

export default asLayerObject({
    id: 'point_icon',
    type: 'symbol',
    sourceLayer: 'point',
    sourceQueries: [
        {
            "minzoom": 1, "maxzoom": 2, "from": "osm_point_z$zoom",
            "filter": ["==", ["get", "place"], "country"]
        },
        {"minzoom": 2, "maxzoom": 20, "from": "osm_point_z$zoom", "drawable": true},
    ],
    // From zoom 16 and not 14. This layer draws two hundred classes of point, and at 14 a city
    // centre came out as a field of icons with the streets underneath them: the roads, the
    // buildings and the street names were all there and none of them could be read. Raising it
    // also empties the tiles at 14 and 15, because the attributes a tile carries are the ones the
    // style reads at that zoom.
    'minzoom': 16,
    layout: {
        'icon-anchor': 'bottom',
        'text-font': ['Noto Sans Regular'],
        'text-size': 12,
        'text-field': ['get', 'name'],
        'text-anchor': 'top',
        'text-optional': true,
        'text-max-width': 5,
    },
    paint: {
        'icon-halo-color': theme.pointIconHaloColor,
        'icon-halo-width': 1,
        // The icon carries the hue and the label does not. Every directive used to set its label in
        // its own class colour, which put two hundred and fifty six names on the map in sixteen
        // hues, four of them too pale against their halo for text that size: a bright blue that
        // reads as a station at icon size is a bright blue that cannot be read as a word. A graphic
        // has to clear 3:1 and a letter 4.5:1, so the two part company here. The category is said
        // once, by the icon standing over the name, and saying it twice was costing the name.
        'text-color': theme.pointTextColor,
        'text-halo-width': 1.5,
        'text-halo-blur': 0.5,
        'text-halo-color': theme.pointTextHaloColor,
    },
    /**
     * These directives are based on the following source:
     * https://wiki.openstreetmap.org/wiki/OpenStreetMap_Carto/Symbols
     *
     * The order they are written in is the order they are matched in: the chain answers with its
     * first hit, so a class is written above the classes it would otherwise claim. That makes the
     * list an order of specificity, grouped by subject, and it cannot also be an order of
     * importance. Read as one it said that the first subject written mattered most, and a
     * restaurant beat a hospital, a station and a pharmacy for the space.
     *
     * So each directive declares what its class is worth as a `priority`, and the position it is
     * written in separates it from the classes of the same standing. Where two icons want the same
     * piece of the screen MapLibre keeps the one with the lower `symbol-sort-key`; a class with no
     * band at all left that to the order the two happened to come out of the database, which
     * changed between tiles and between imports of the same extract.
     */
    directives: withSymbolSortKeys(arriving([
        // Gastronomy
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'restaurant'],
                ['==', ['get', 'amenity'], 'food_court']
            ],
            'icon-image': 'restaurant',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'cafe'],
            'icon-image': 'cafe',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fast_food'],
            'icon-image': 'fast_food',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bar'],
            'icon-image': 'bar',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'pub'],
            'icon-image': 'pub',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'ice_cream'],
            'icon-image': 'ice_cream',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'biergarten'],
            'icon-image': 'biergarten',
            'priority': PRIORITY.commerce,
            'icon-color': theme.gastronomyIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'outdoor_seating'],
            'icon-image': 'outdoor_seating',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },

        // Culture, entertainment, and arts
        {
            'filter': ['==', ['get', 'tourism'], 'artwork'],
            'icon-image': 'artwork',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'community_centre'],
            'icon-image': 'community_centre',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'library'],
            'icon-image': 'library',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'museum'],
            'icon-image': 'museum',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'theatre'],
            'icon-image': 'theatre',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'cinema'],
            'icon-image': 'cinema',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'nightclub'],
            'icon-image': 'nightclub',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'arts_centre'],
            'icon-image': 'arts_centre',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'gallery'],
            'icon-image': 'art',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'internet_cafe'],
            'icon-image': 'internet_cafe',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'casino'],
            'icon-image': 'casino',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'public_bookcase'],
            'icon-image': 'public_bookcase',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'amusement_arcade'],
            'icon-image': 'amusement_arcade',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },

        // Historical objects
        {
            'filter': ['==', ['get', 'historic'], 'archaeological_site'],
            'icon-image': 'archaeological_site',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': ['==', ['get', 'historic'], 'wayside_shrine'],
            'icon-image': 'wayside_shrine',
            'priority': PRIORITY.detail,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': ['==', ['get', 'historic'], 'monument'],
            'icon-image': 'monument',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'historic'], 'memorial'],
                    ['==', ['get', 'memorial'], 'plaque']
                ],
                [
                    'all',
                    ['==', ['get', 'historic'], 'memorial'],
                    ['==', ['get', 'memorial'], 'blue_plaque']
                ]
            ],
            'icon-image': 'plaque',
            'priority': PRIORITY.detail,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'historic'], 'memorial'],
                    ['==', ['get', 'memorial'], 'statue']
                ],
                [
                    'all',
                    ['==', ['get', 'tourism'], 'artwork'],
                    ['==', ['get', 'artwork_type'], 'statue']
                ]
            ],
            'icon-image': 'statue',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'historic'], 'memorial'],
                ['==', ['get', 'memorial'], 'stone']
            ],
            'icon-image': 'stone',
            'priority': PRIORITY.detail,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'historic'], 'castle'],
                    ['==', ['get', 'castle_type'], 'palace']
                ],
                [
                    'all',
                    ['==', ['get', 'historic'], 'castle'],
                    ['==', ['get', 'castle_type'], 'stately']
                ]
            ],
            'icon-image': 'palace',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        // {
        //     'filter': ['==', ['get', 'historic'], 'castle'], =>defensive / =>fortress / =>castrum / =>shiro / =>kremlin
        //     'icon-image': 'fortress',
        //     'icon-color': theme.historyIconColor,
        // },
        {
            'filter': ['==', ['get', 'historic'], 'fort'],
            'icon-image': 'historic_fort',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'historic'], 'memorial'],
                    ['==', ['get', 'memorial'], 'bust']
                ],
                [
                    'all',
                    ['==', ['get', 'tourism'], 'artwork'],
                    ['==', ['get', 'artwork_type'], 'bust']
                ]
            ],
            'icon-image': 'bust',
            'priority': PRIORITY.detail,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': ['==', ['get', 'historic'], 'city_gate'],
            'icon-image': 'city_gate',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'historic'], 'manor'],
                [
                    'all',
                    ['==', ['get', 'historic'], 'castle'],
                    ['==', ['get', 'castle_type'], 'manor']
                ]
            ],
            'icon-image': 'manor',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'obelisk'],
            'icon-image': 'obelisk',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },
        // A memorial or a castle that says nothing more about itself, below the kinds of each that
        // do. Written above them, these two claimed the plaques, the statues, the busts, the
        // palaces and the manors before their own directives were reached.
        {
            'filter': ['==', ['get', 'historic'], 'memorial'],
            'icon-image': 'memorial',
            'priority': PRIORITY.detail,
            'icon-color': theme.historyIconColor,
        },
        {
            'filter': ['==', ['get', 'historic'], 'castle'],
            'icon-image': 'castle',
            'priority': PRIORITY.landmark,
            'icon-color': theme.historyIconColor,
        },

        // Leisure, recreation, and sport
        {
            'filter': ['==', ['get', 'leisure'], 'playground'],
            'icon-image': 'playground',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'leisure'], 'fitness_centre'],
                ['==', ['get', 'leisure'], 'fitness_station']
            ],
            'icon-image': 'fitness',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'golf_course'],
            'icon-image': 'golf',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'leisure'], 'water_park'],
                ['==', ['get', 'leisure'], 'swimming_area'],
                [
                    'all',
                    ['==', ['get', 'leisure'], 'sports_centre'],
                    ['==', ['get', 'sport'], 'swimming']
                ]
            ],
            'icon-image': 'water_park',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'massage'],
            'icon-image': 'massage',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'sauna'],
            'icon-image': 'sauna',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'public_bath'],
            'icon-image': 'public_bath',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityPublicBathIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'miniature_golf'],
            'icon-image': 'miniature_golf',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'beach_resort'],
            'icon-image': 'beach_resort',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'fishing'],
            'icon-image': 'fishing',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'bowling_alley'],
            'icon-image': 'bowling_alley',
            'priority': PRIORITY.commerce,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'dog_park'],
            'icon-image': 'dog_park',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'golf'], 'pin'],
            'icon-image': 'leisure_golf_pin',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },

        // Waste management
        {
            'filter': ['==', ['get', 'amenity'], 'toilets'],
            'icon-image': 'toilets',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'recycling'],
            'icon-image': 'recycling',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'waste_basket'],
            'icon-image': 'waste_basket',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'waste_disposal'],
            'icon-image': 'waste_disposal',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'excrement_bags']
            ],
            'icon-image': 'excrement_bags',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },

        // Outdoor
        {
            'filter': ['==', ['get', 'amenity'], 'bench'],
            'icon-image': 'bench',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'shelter'],
            'icon-image': 'shelter',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'drinking_water'],
            'icon-image': 'drinking_water',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'picnic_site'],
            'icon-image': 'picnic',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fountain'],
            'icon-image': 'fountain',
            'priority': PRIORITY.detail,
            'icon-color': theme.waterIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'camp_site'],
            'icon-image': 'camping',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'picnic_table'],
            'icon-image': 'picnic',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'caravan_site'],
            'icon-image': 'caravan_park',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bbq'],
            'icon-image': 'bbq',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'shower'],
            'icon-image': 'shower',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'firepit'],
            'icon-image': 'firepit',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'bird_hide'],
            'icon-image': 'bird_hide',
            'priority': PRIORITY.detail,
            'icon-color': theme.leisureIconColor,
        },

        // Tourism and accommodation
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'guidepost']
            ],
            'icon-image': 'guidepost',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'board']
            ],
            'icon-image': 'board',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'tourism'], 'information'],
                    ['==', ['get', 'information'], 'map']
                ],
                [
                    'all',
                    ['==', ['get', 'tourism'], 'information'],
                    ['==', ['get', 'information'], 'tactile_map']
                ]
            ],
            'icon-image': 'map',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'office']
            ],
            'icon-image': 'office',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'terminal']
            ],
            'icon-image': 'terminal',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'audioguide']
            ],
            'icon-image': 'audioguide',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'viewpoint'],
            'icon-image': 'viewpoint',
            'priority': PRIORITY.landmark,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'hotel'],
            'icon-image': 'hotel',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'guest_house'],
            'icon-image': 'guest_house',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'hostel'],
            'icon-image': 'hostel',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'chalet'],
            'icon-image': 'chalet',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'motel'],
            'icon-image': 'motel',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'apartment'],
            'icon-image': 'apartment',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'alpine_hut'],
            'icon-image': 'alpinehut',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'wilderness_hut'],
            'icon-image': 'wilderness_hut',
            'priority': PRIORITY.commerce,
            'icon-color': theme.accommodationIconColor,
        },

        // Finance
        {
            'filter': ['==', ['get', 'amenity'], 'bank'],
            'icon-image': 'bank',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'atm'],
            'icon-image': 'atm',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bureau_de_change'],
            'icon-image': 'bureau_de_change',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },

        // Healthcare
        {
            'filter': ['==', ['get', 'amenity'], 'pharmacy'],
            'icon-image': 'pharmacy',
            'priority': PRIORITY.emergency,
            'icon-color': theme.healthIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'hospital'],
            'icon-image': 'hospital',
            'priority': PRIORITY.emergency,
            'icon-color': theme.healthIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'clinic'],
                ['==', ['get', 'amenity'], 'doctors']
            ],
            'icon-image': 'doctors',
            'priority': PRIORITY.emergency,
            'icon-color': theme.healthIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'dentist'],
            'icon-image': 'dentist',
            'priority': PRIORITY.commerce,
            'icon-color': theme.healthIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'veterinary'],
            'icon-image': 'veterinary',
            'priority': PRIORITY.commerce,
            'icon-color': theme.healthIconColor,
        },

        // Communication
        {
            'filter': ['==', ['get', 'amenity'], 'post_box'],
            'icon-image': 'post_box',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'post_office'],
            'icon-image': 'post_office',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        // {
        //     'filter': ['==', ['get', 'amenity'], 'parcel_locker'],
        //     'icon-image': 'parcel_locker',
        //     'icon-color': theme.amenityIconColor,
        // },
        {
            'filter': ['==', ['get', 'amenity'], 'telephone'],
            'icon-image': 'telephone',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'emergency'], 'phone'],
            'icon-image': 'emergency_phone',
            'priority': PRIORITY.emergency,
            'icon-color': theme.amenityIconColor,
        },

        // Transportation
        {
            'filter': [
                'any',
                ['all',
                    ['==', ['get', 'amenity'], 'parking'],
                    ['==', ['get', 'parking'], 'lane'],
                ],
                ['all',
                    ['==', ['get', 'amenity'], 'parking'],
                    ['==', ['get', 'parking'], 'street_side']
                ]
            ],
            'icon-image': 'parking_subtle',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'parking'],
            'icon-image': 'parking',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'highway'], 'bus_stop'],
            'icon-image': 'bus_stop',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fuel'],
            'icon-image': 'fuel',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_parking'],
            'icon-image': 'bicycle_parking',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'railway'], 'station'],
                ['==', ['get', 'railway'], 'halt'],
                ['==', ['get', 'railway'], 'tram_stop']
            ],
            'icon-image': 'place-6',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bus_station'],
            'icon-image': 'bus_station',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'aeroway'], 'helipad'],
            'icon-image': 'helipad',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportDefaultIconColor,
        },
        {
            'filter': ['==', ['get', 'aeroway'], 'aerodrome'],
            'icon-image': 'aerodrome',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportDefaultIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_rental'],
            'icon-image': 'rental_bicycle',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'slipway'],
            'icon-image': 'slipway',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'taxi'],
            'icon-image': 'taxi',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'parking_tickets']
            ],
            'icon-image': 'parking_tickets',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'railway'], 'subway_entrance'],
            'icon-image': 'entrance',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'charging_station'],
            'icon-image': 'charging_station',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'highway'], 'elevator'],
            'icon-image': 'elevator',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'car_rental'],
            'icon-image': 'rental_car',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'parking_entrance'],
                ['==', ['get', 'parking'], 'underground']
            ],
            'icon-image': 'parking_entrance_underground',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'public_transport_tickets']
            ],
            'icon-image': 'public_transport_tickets',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'ferry_terminal'],
            'icon-image': 'ferry',
            'priority': PRIORITY.interchange,
            'icon-color': theme.transportDefaultIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'motorcycle_parking'],
            'icon-image': 'motorcycle_parking',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_repair_station'],
            'icon-image': 'bicycle_repair_station',
            'priority': PRIORITY.detail,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'boat_rental'],
            'icon-image': 'boat_rental',
            'priority': PRIORITY.transport,
            'icon-color': theme.transportationIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'parking_entrance'],
                ['==', ['get', 'parking'], 'multi-storey']
            ],
            'icon-image': 'parking_entrance_multistorey',
            'priority': PRIORITY.detail,
            'icon-color': theme.transportationIconColor,
        },

        // Road features
        // {
        //     'filter': ['==', ['get', 'oneway'], 'yes'],
        //     'icon-image': 'oneway',
        //     'icon-color': theme.defaultIconColor,
        // },
        {
            'filter': ['==', ['get', 'barrier'], 'gate'],
            'icon-image': 'gate',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'highway'], 'traffic_signals'],
            'icon-image': 'traffic_light',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        // {
        //     'filter': [
        //         'any',
        //         ['==', ['get', 'railway'], 'level_crossing'],
        //         ['==', ['get', 'railway'], 'crossing']
        //     ],
        //     'icon-image': 'level_crossing2',
        //     'icon-color': theme.defaultIconColor,
        // },
        {
            'filter': [
                'any',
                ['==', ['get', 'railway'], 'level_crossing'],
                ['==', ['get', 'railway'], 'crossing']
            ],
            'icon-image': 'level_crossing',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'barrier'], 'bollard'],
                ['==', ['get', 'barrier'], 'block'],
                ['==', ['get', 'barrier'], 'turnstile'],
                ['==', ['get', 'barrier'], 'log']
            ],
            'icon-image': 'gate',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'barrier'], 'lift_gate'],
                ['==', ['get', 'barrier'], 'swing_gate']
            ],
            'icon-image': 'lift_gate',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'cycle_barrier'],
            'icon-image': 'cycle_barrier',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'stile'],
            'icon-image': 'stile',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        // {
        //     'filter': ['==', ['get', 'highway'], 'mini_roundabout'],
        //     'icon-image': 'highway_mini_roundabout',
        //     'icon-color': theme.defaultIconColor,
        // },
        {
            'filter': ['==', ['get', 'barrier'], 'toll_booth'],
            'icon-image': 'toll_booth',
            'priority': PRIORITY.detail,
            'icon-color': theme.accommodationIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'cattle_grid'],
            'icon-image': 'barrier_cattle_grid',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'kissing_gate'],
            'icon-image': 'kissing_gate',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'full-height_turnstile'],
            'icon-image': 'full-height_turnstile',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'barrier'], 'motorcycle_barrier'],
            'icon-image': 'motorcycle_barrier',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'ford'], 'yes'],
                ['==', ['get', 'ford'], 'stepping_stones']
            ],
            'icon-image': 'ford',
            'priority': PRIORITY.detail,
            'icon-color': theme.waterIconColor,
        },
        // {
        //     'filter': ['==', ['get', 'mountain_pass'], 'yes'],
        //     'icon-image': 'mountain_pass',
        //     'icon-color': theme.transportationIconColor,
        // },
        {
            'filter': ['==', ['get', 'waterway'], 'dam'],
            'icon-image': 'place-6',
            'priority': PRIORITY.landmark,
            'icon-color': theme.waterIconColor,
        },
        {
            'filter': ['==', ['get', 'waterway'], 'weir'],
            'icon-image': 'place-6',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'waterway'], 'lock_gate'],
            'icon-image': 'place-6',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
        // {
        //     'filter': ['==', ['get', 'Node with highway'], 'turning_circle at way with highway'],
        //     'icon-image': 'turning_circle_on_highway_track',
        //     'icon-color': theme.defaultIconColor,
        // },

        // Nature. Summits are drawn by `point_summit`, which reaches lower than this layer does
        // and carries their elevation.
        {
            'filter': ['==', ['get', 'natural'], 'spring'],
            'icon-image': 'spring',
            'priority': PRIORITY.landmark,
            'icon-color': theme.waterIconColor,
        },
        {
            'filter': ['==', ['get', 'natural'], 'cave_entrance'],
            'icon-image': 'cave',
            'priority': PRIORITY.landmark,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'waterway'], 'waterfall'],
            'icon-image': 'waterfall',
            'priority': PRIORITY.landmark,
            'icon-color': theme.waterIconColor,
        },
        {
            'filter': ['==', ['get', 'natural'], 'saddle'],
            'icon-image': 'saddle',
            'priority': PRIORITY.landmark,
            'icon-color': theme.defaultIconColor,
        },

        // Administrative facilities
        {
            'filter': ['==', ['get', 'amenity'], 'police'],
            'icon-image': 'police',
            'priority': PRIORITY.emergency,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'townhall'],
            'icon-image': 'town_hall',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fire_station'],
            'icon-image': 'firestation',
            'priority': PRIORITY.emergency,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'social_facility'],
            'icon-image': 'social_facility',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'courthouse'],
            'icon-image': 'courthouse',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'office'], 'diplomatic'],
                ['==', ['get', 'diplomatic'], 'embassy']
            ],
            'icon-image': 'diplomatic',
            'priority': PRIORITY.civic,
            'icon-color': theme.officeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'office'], 'diplomatic'],
                ['==', ['get', 'diplomatic'], 'consulate']
            ],
            'icon-image': 'consulate',
            'priority': PRIORITY.civic,
            'icon-color': theme.officeIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'prison'],
            'icon-image': 'prison',
            'priority': PRIORITY.civic,
            'icon-color': theme.amenityIconColor,
        },

        // Religious place
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'christian']
            ],
            'icon-image': 'christian',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'jewish']
            ],
            'icon-image': 'jewish',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'muslim']
            ],
            'icon-image': 'muslim',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'taoist']
            ],
            'icon-image': 'taoist',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'hindu']
            ],
            'icon-image': 'hinduist',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'buddhist']
            ],
            'icon-image': 'buddhist',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'shinto']
            ],
            'icon-image': 'shintoist',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'sikh']
            ],
            'icon-image': 'sikhist',
            'priority': PRIORITY.landmark,
            'icon-color': theme.religionIconColor,
        },
        // {
        //     'filter': [
        //         'all',
        //         ['==', ['get', 'amenity'], 'place_of_worship'],
        //         ['==', ['get', 'without or other religion'], '* value']
        //     ],
        //     'icon-image': 'place_of_worship',
        //     'icon-color': theme.religionIconColor,
        // },

        // Shop and services
        {
            'filter': ['==', ['get', 'amenity'], 'marketplace'],
            'icon-image': 'marketplace',
            'priority': PRIORITY.civic,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'convenience'],
            'icon-image': 'convenience',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'supermarket'],
            'icon-image': 'supermarket',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'clothes'],
                ['==', ['get', 'shop'], 'fashion']
            ],
            'icon-image': 'clothes',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'hairdresser'],
            'icon-image': 'hairdresser',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bakery'],
            'icon-image': 'bakery',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car_repair'],
            'icon-image': 'car_repair',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'doityourself'],
                ['==', ['get', 'shop'], 'hardware']
            ],
            'icon-image': 'diy',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car'],
            'icon-image': 'car',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'kiosk'],
                ['==', ['get', 'shop'], 'newsagent']
            ],
            'icon-image': 'newsagent',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'beauty'],
            'icon-image': 'beauty',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'car_wash'],
            'icon-image': 'car_wash',
            'priority': PRIORITY.commerce,
            'icon-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'butcher'],
            'icon-image': 'butcher',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'alcohol'],
                ['==', ['get', 'shop'], 'wine']
            ],
            'icon-image': 'alcohol',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'furniture'],
            'icon-image': 'furniture',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'florist'],
            'icon-image': 'florist',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'mobile_phone'],
            'icon-image': 'mobile_phone',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'electronics'],
            'icon-image': 'electronics',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'shoes'],
            'icon-image': 'shoes',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car_parts'],
            'icon-image': 'car_parts',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'greengrocer'],
                ['==', ['get', 'shop'], 'farm']
            ],
            'icon-image': 'greengrocer',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'laundry'],
                ['==', ['get', 'shop'], 'dry_cleaning']
            ],
            'icon-image': 'laundry',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'optician'],
            'icon-image': 'optician',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'jewelry'],
                ['==', ['get', 'shop'], 'jewellery']
            ],
            'icon-image': 'jewelry',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'books'],
            'icon-image': 'library',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'gift'],
            'icon-image': 'gift',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'department_store'],
            'icon-image': 'department_store',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bicycle'],
            'icon-image': 'bicycle',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'confectionery'],
                ['==', ['get', 'shop'], 'chocolate'],
                ['==', ['get', 'shop'], 'pastry']
            ],
            'icon-image': 'confectionery',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'variety_store'],
            'icon-image': 'variety_store',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'travel_agency'],
            'icon-image': 'travel_agency',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'sports'],
            'icon-image': 'sports',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'chemist'],
            'icon-image': 'chemist',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'computer'],
            'icon-image': 'computer',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'stationery'],
            'icon-image': 'stationery',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'pet'],
            'icon-image': 'pet',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'beverages'],
            'icon-image': 'beverages',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'cosmetics'],
                ['==', ['get', 'shop'], 'perfumery']
            ],
            'icon-image': 'perfumery',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tyres'],
            'icon-image': 'tyres',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'motorcycle'],
            'icon-image': 'motorcycle',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'garden_centre'],
            'icon-image': 'garden_centre',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'copyshop'],
            'icon-image': 'copyshop',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'toys'],
            'icon-image': 'toys',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'deli'],
            'icon-image': 'deli',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tobacco'],
            'icon-image': 'tobacco',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'seafood'],
            'icon-image': 'seafood',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'interior_decoration'],
            'icon-image': 'interior_decoration',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'ticket'],
            'icon-image': 'ticket',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'photo'],
                ['==', ['get', 'shop'], 'photo_studio'],
                ['==', ['get', 'shop'], 'photography']
            ],
            'icon-image': 'photo',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'trade'],
                ['==', ['get', 'shop'], 'wholesale']
            ],
            'icon-image': 'trade',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'outdoor'],
            'icon-image': 'outdoor',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'houseware'],
            'icon-image': 'houseware',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'art'],
            'icon-image': 'art',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'paint'],
            'icon-image': 'paint',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'fabric'],
            'icon-image': 'fabric',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bookmaker'],
            'icon-image': 'bookmaker',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'second_hand'],
            'icon-image': 'second_hand',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'charity'],
            'icon-image': 'charity',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bed'],
            'icon-image': 'bed',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'medical_supply'],
            'icon-image': 'medical_supply',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'hifi'],
            'icon-image': 'hifi',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'music'],
            'icon-image': 'music',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'coffee'],
            'icon-image': 'coffee',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'musical_instrument'],
            'icon-image': 'musical_instrument',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tea'],
            'icon-image': 'tea',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'video'],
            'icon-image': 'video',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bag'],
            'icon-image': 'bag',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'carpet'],
            'icon-image': 'carpet',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'video_games'],
            'icon-image': 'video_games',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'vehicle_inspection'],
            'icon-image': 'vehicle_inspection',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'dairy'],
            'icon-image': 'dairy',
            'priority': PRIORITY.commerce,
            'icon-color': theme.shopIconColor,
        },
        // {
        //     'filter': ['!=', ['get', 'shop'], 'yes'],
        //     'icon-image': 'place-4',
        // 'icon-color': theme.shopIconColor,
        // },
        {
            'filter': ['==', ['get', 'office'], '*'],
            'icon-image': 'office',
            'priority': PRIORITY.commerce,
            'icon-color': theme.defaultIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'nursing_home'],
                ['==', ['get', 'amenity'], 'childcare']
            ],
            'icon-image': 'place-6',
            'priority': PRIORITY.civic,
            'icon-color': theme.defaultIconColor,
        },

        // Landmarks, man-made infrastructure, masts and towers
        {
            'filter': ['any', ['==', ['get', 'man_made'], 'storage_tank'], ['==', ['get', 'man_made'], 'silo']],
            'icon-image': 'storage_tank',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        // A tower named by both its purpose and its construction is the narrowest thing said about
        // it, so these two come before the directives that name only one of the pair.
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'communication'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice_communication',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'lighting'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice_lighting',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['all', ['==', ['get', 'man_made'], 'tower'], ['==', ['get', 'tower:type'], 'communication']],
            'icon-image': 'tower_cantilever_communication',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'power'], 'generator'],
                ['any',
                    ['==', ['get', 'generator:source'], 'wind'],
                    ['==', ['get', 'generator:method'], 'wind_turbine']
                ]
            ],
            'icon-image': 'generator_wind',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'hunting_stand'],
            'icon-image': 'hunting_stand',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['any', ['==', ['get', 'historic'], 'wayside_cross'], ['==', ['get', 'man_made'], 'cross']],
            'icon-image': 'christian',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'water_tower'],
            'icon-image': 'water_tower',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            filter: ['==', ['get', 'military'], 'bunker'],
            'icon-image': 'bunker',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'chimney'],
            'icon-image': 'chimney',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'any',
                [
                    'all',
                    ['==', ['get', 'man_made'], 'tower'],
                    ['==', ['get', 'tower:type'], 'observation']
                ],
                [
                    'all',
                    ['==', ['get', 'man_made'], 'tower'],
                    ['==', ['get', 'tower:type'], 'watchtower']
                ]
            ],
            'icon-image': 'tower_observation',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'bell_tower']
            ],
            'icon-image': 'tower_bell_tower',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'lighting']
            ],
            'icon-image': 'tower_lighting',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'lighthouse'],
            'icon-image': 'lighthouse',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'advertising'], 'column'],
            'icon-image': 'column',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'crane'],
            'icon-image': 'crane',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'windmill'],
            'icon-image': 'windmill',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'mast'],
                ['==', ['get', 'tower:type'], 'lighting']
            ],
            'icon-image': 'mast_lighting',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'mast'],
                ['==', ['get', 'tower:type'], 'communication']
            ],
            'icon-image': 'mast_communications',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'communications_tower'],
            'icon-image': 'communication_tower',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'defensive']
            ],
            'icon-image': 'tower_defensive',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'cooling']
            ],
            'icon-image': 'tower_cooling',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'dish']
            ],
            'icon-image': 'tower_dish',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'dome']
            ],
            'icon-image': 'tower_dome',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        // A tower or a mast that says nothing more about itself. Written below every kind of
        // tower and not above them: a chain answers with its first hit, and a directive asking
        // only for `man_made=tower` claims the communication towers, the lattice towers, the
        // watchtowers and the cooling towers before any of them is reached, leaving fourteen
        // sprites in the sheet that nothing ever drew.
        {
            'filter': ['==', ['get', 'man_made'], 'tower'],
            'icon-image': 'tower_generic',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'mast'],
            'icon-image': 'mast',
            'priority': PRIORITY.detail,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'telescope'],
                ['==', ['get', 'telescope:type'], 'radio']
            ],
            'icon-image': 'telescope_dish',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'telescope'],
                ['==', ['get', 'telescope:type'], 'optical']
            ],
            'icon-image': 'telescope_dome',
            'priority': PRIORITY.landmark,
            'icon-color': theme.manMadeIconColor,
        },

        // Electricity
        // {
        //     'filter': ['==', ['get', 'power'], 'tower'],
        //     'icon-image': 'power_tower',
        //     'icon-color': theme.powerIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'power'], 'pole'],
        //     'icon-image': 'place-6',
        //     'icon-color': theme.powerIconColor,
        // },

        // Places
        {
            'filter': ['==', ['get', 'place'], 'city'],
            'icon-image': 'place-6',
            'priority': PRIORITY.landmark,
            'icon-color': theme.defaultIconColor,
        },
        // {
        //     'filter': ['has', 'capital'],
        //     'icon-image': 'place_capital',
        //     'icon-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'yes'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'main'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'service'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        // },
        {
            'filter': [
                'all',
                ['has', 'entrance'],
                ['==', ['get', 'access'], 'no']
            ],
            'icon-image': 'entrance',
            'priority': PRIORITY.detail,
            'icon-color': theme.defaultIconColor,
        },
    ])),
});
