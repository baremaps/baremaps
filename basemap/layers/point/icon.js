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
import {asLayerObject, withSortKeys} from '../../utils/utils.js';
import theme from '../../theme.js';

export default asLayerObject({
    id: 'icon',
    type: 'symbol',
    sourceLayer: 'point',
    sourceQueries: [
        {
            "minzoom": 1, "maxzoom": 2, "from": "osm_point_z$zoom",
            "filter": ["==", ["get", "place"], "country"]
        },
        {"minzoom": 2, "maxzoom": 20, "from": "osm_point_z$zoom", "drawable": true},
    ],
    'minzoom': 14,
    layout: {
        visibility: 'visible',
        'icon-size': 1,
        'icon-anchor': 'bottom',
        'text-font': ['Noto Sans Regular'],
        'text-size': 11,
        'text-field': ['get', 'name'],
        'text-anchor': 'top',
        'text-optional': true,
        'text-max-width': 5,
    },
    paint: {
        'icon-opacity': 1,
        'icon-translate-anchor': 'map',
        'icon-halo-color': theme.pointIconHaloColor,
        'icon-halo-width': 1,
        'text-halo-width': 1,
        'text-halo-color': theme.pointTextHaloColor,
    },
    /**
     * These directives are based on the following source:
     * https://wiki.openstreetmap.org/wiki/OpenStreetMap_Carto/Symbols
     */
    directives: withSortKeys([
        // Gastronomy
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'restaurant'],
                ['==', ['get', 'amenity'], 'food_court']
            ],
            'icon-image': 'restaurant',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'cafe'],
            'icon-image': 'cafe',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fast_food'],
            'icon-image': 'fast_food',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bar'],
            'icon-image': 'bar',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'pub'],
            'icon-image': 'pub',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'ice_cream'],
            'icon-image': 'ice_cream',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'biergarten'],
            'icon-image': 'biergarten',
            'icon-color': theme.gastronomyIconColor,
            'text-color': theme.gastronomyIconColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'outdoor_seating'],
            'icon-image': 'outdoor_seating',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },

        // Culture, entertainment, and arts
        {
            'filter': ['==', ['get', 'tourism'], 'artwork'],
            'icon-image': 'artwork',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'community_centre'],
            'icon-image': 'community_centre',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'library'],
            'icon-image': 'library',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'museum'],
            'icon-image': 'museum',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'theatre'],
            'icon-image': 'theatre',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'cinema'],
            'icon-image': 'cinema',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'nightclub'],
            'icon-image': 'nightclub',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'arts_centre'],
            'icon-image': 'arts_centre',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'gallery'],
            'icon-image': 'art',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'internet_cafe'],
            'icon-image': 'internet_cafe',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'casino'],
            'icon-image': 'casino',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'public_bookcase'],
            'icon-image': 'public_bookcase',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'amusement_arcade'],
            'icon-image': 'amusement_arcade',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },

        // Historical objects
        {
            'filter': ['==', ['get', 'historic'], 'memorial'],
            'icon-image': 'memorial',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'historic'], 'archaeological_site'],
            'icon-image': 'archaeological_site',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'historic'], 'wayside_shrine'],
            'icon-image': 'wayside_shrine',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'historic'], 'monument'],
            'icon-image': 'monument',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'historic'], 'castle'],
            'icon-image': 'castle',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
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
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
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
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'historic'], 'memorial'],
                ['==', ['get', 'memorial'], 'stone']
            ],
            'icon-image': 'stone',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
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
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        // {
        //     'filter': ['==', ['get', 'historic'], 'castle'], =>defensive / =>fortress / =>castrum / =>shiro / =>kremlin
        //     'icon-image': 'fortress',
        //     'icon-color': theme.historyIconColor,
        //     'text-color': theme.historyIconColor
        // },
        {
            'filter': ['==', ['get', 'historic'], 'fort'],
            'icon-image': 'historic_fort',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
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
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'historic'], 'city_gate'],
            'icon-image': 'city_gate',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
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
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },
        {
            'filter': ['==', ['get', 'man_made'], 'obelisk'],
            'icon-image': 'obelisk',
            'icon-color': theme.historyIconColor,
            'text-color': theme.historyIconColor
        },

        // Leisure, recreation, and sport
        {
            'filter': ['==', ['get', 'leisure'], 'playground'],
            'icon-image': 'playground',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'leisure'], 'fitness_centre'],
                ['==', ['get', 'leisure'], 'fitness_station']
            ],
            'icon-image': 'fitness',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'golf_course'],
            'icon-image': 'golf',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
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
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'massage'],
            'icon-image': 'massage',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'sauna'],
            'icon-image': 'sauna',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'public_bath'],
            'icon-image': 'public_bath',
            'icon-color': theme.amenityPublicBathIconColor,
            'text-color': theme.amenityPublicBathTextColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'miniature_golf'],
            'icon-image': 'miniature_golf',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'beach_resort'],
            'icon-image': 'beach_resort',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'fishing'],
            'icon-image': 'fishing',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'bowling_alley'],
            'icon-image': 'bowling_alley',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'dog_park'],
            'icon-image': 'dog_park',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },
        {
            'filter': ['==', ['get', 'golf'], 'pin'],
            'icon-image': 'leisure_golf_pin',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },

        // Waste management
        {
            'filter': ['==', ['get', 'amenity'], 'toilets'],
            'icon-image': 'toilets',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'recycling'],
            'icon-image': 'recycling',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'waste_basket'],
            'icon-image': 'waste_basket',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'waste_disposal'],
            'icon-image': 'waste_disposal',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'excrement_bags']
            ],
            'icon-image': 'excrement_bags',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },

        // Outdoor
        {
            'filter': ['==', ['get', 'amenity'], 'bench'],
            'icon-image': 'bench',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'shelter'],
            'icon-image': 'shelter',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'drinking_water'],
            'icon-image': 'drinking_water',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'picnic_site'],
            'icon-image': 'picnic',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fountain'],
            'icon-image': 'fountain',
            'icon-color': theme.waterIconColor,
            'text-color': theme.waterIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'camp_site'],
            'icon-image': 'camping',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'picnic_table'],
            'icon-image': 'picnic',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor,
        },
        {
            'filter': ['==', ['get', 'tourism'], 'caravan_site'],
            'icon-image': 'caravan_park',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bbq'],
            'icon-image': 'bbq',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'shower'],
            'icon-image': 'shower',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'firepit'],
            'icon-image': 'firepit',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'leisure'], 'bird_hide'],
            'icon-image': 'bird_hide',
            'icon-color': theme.leisureIconColor,
            'text-color': theme.leisureIconColor,
        },

        // Tourism and accommodation
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'guidepost']
            ],
            'icon-image': 'guidepost',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'board']
            ],
            'icon-image': 'board',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
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
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'office']
            ],
            'icon-image': 'office',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'terminal']
            ],
            'icon-image': 'terminal',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'tourism'], 'information'],
                ['==', ['get', 'information'], 'audioguide']
            ],
            'icon-image': 'audioguide',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'viewpoint'],
            'icon-image': 'viewpoint',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'hotel'],
            'icon-image': 'hotel',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'guest_house'],
            'icon-image': 'guest_house',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'hostel'],
            'icon-image': 'hostel',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'chalet'],
            'icon-image': 'chalet',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'motel'],
            'icon-image': 'motel',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'apartment'],
            'icon-image': 'apartment',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'alpine_hut'],
            'icon-image': 'alpinehut',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'tourism'], 'wilderness_hut'],
            'icon-image': 'wilderness_hut',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },

        // Finance
        {
            'filter': ['==', ['get', 'amenity'], 'bank'],
            'icon-image': 'bank',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'atm'],
            'icon-image': 'atm',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bureau_de_change'],
            'icon-image': 'bureau_de_change',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },

        // Healthcare
        {
            'filter': ['==', ['get', 'amenity'], 'pharmacy'],
            'icon-image': 'pharmacy',
            'icon-color': theme.healthIconColor,
            'text-color': theme.healthIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'hospital'],
            'icon-image': 'hospital',
            'icon-color': theme.healthIconColor,
            'text-color': theme.healthIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'clinic'],
                ['==', ['get', 'amenity'], 'doctors']
            ],
            'icon-image': 'doctors',
            'icon-color': theme.healthIconColor,
            'text-color': theme.healthIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'dentist'],
            'icon-image': 'dentist',
            'icon-color': theme.healthIconColor,
            'text-color': theme.healthIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'veterinary'],
            'icon-image': 'veterinary',
            'icon-color': theme.healthIconColor,
            'text-color': theme.healthIconColor
        },

        // Communication
        {
            'filter': ['==', ['get', 'amenity'], 'post_box'],
            'icon-image': 'post_box',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'post_office'],
            'icon-image': 'post_office',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        // {
        //     'filter': ['==', ['get', 'amenity'], 'parcel_locker'],
        //     'icon-image': 'parcel_locker',
        //     'icon-color': theme.amenityIconColor,
        //     'text-color': theme.amenityIconColor
        // },
        {
            'filter': ['==', ['get', 'amenity'], 'telephone'],
            'icon-image': 'telephone',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'emergency'], 'phone'],
            'icon-image': 'emergency_phone',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },

        // Transportation
        {
            'filter': ['==', ['get', 'amenity'], 'parking'],
            'icon-image': 'parking',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
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
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'highway'], 'bus_stop'],
            'icon-image': 'bus_stop',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fuel'],
            'icon-image': 'fuel',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_parking'],
            'icon-image': 'bicycle_parking',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'railway'], 'station'],
                ['==', ['get', 'railway'], 'halt'],
                ['==', ['get', 'railway'], 'tram_stop']
            ],
            'icon-image': 'place-6',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bus_station'],
            'icon-image': 'bus_station',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'aeroway'], 'helipad'],
            'icon-image': 'helipad',
            'icon-color': theme.transportDefaultIconColor,
            'text-color': theme.transportDefaultIconColor
        },
        {
            'filter': ['==', ['get', 'aeroway'], 'aerodrome'],
            'icon-image': 'aerodrome',
            'icon-color': theme.transportDefaultIconColor,
            'text-color': theme.transportDefaultIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_rental'],
            'icon-image': 'rental_bicycle',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'leisure'], 'slipway'],
            'icon-image': 'slipway',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'taxi'],
            'icon-image': 'taxi',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'parking_tickets']
            ],
            'icon-image': 'parking_tickets',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'railway'], 'subway_entrance'],
            'icon-image': 'entrance',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'charging_station'],
            'icon-image': 'charging_station',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'highway'], 'elevator'],
            'icon-image': 'elevator',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'car_rental'],
            'icon-image': 'rental_car',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'parking_entrance'],
                ['==', ['get', 'parking'], 'underground']
            ],
            'icon-image': 'parking_entrance_underground',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'vending_machine'],
                ['==', ['get', 'vending'], 'public_transport_tickets']
            ],
            'icon-image': 'public_transport_tickets',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'ferry_terminal'],
            'icon-image': 'ferry',
            'icon-color': theme.transportDefaultIconColor,
            'text-color': theme.transportDefaultIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'motorcycle_parking'],
            'icon-image': 'motorcycle_parking',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'bicycle_repair_station'],
            'icon-image': 'bicycle_repair_station',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'boat_rental'],
            'icon-image': 'boat_rental',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'parking_entrance'],
                ['==', ['get', 'parking'], 'multi-storey']
            ],
            'icon-image': 'parking_entrance_multistorey',
            'icon-color': theme.transportationIconColor,
            'text-color': theme.transportationIconColor
        },

        // Road features
        // {
        //     'filter': ['==', ['get', 'oneway'], 'yes'],
        //     'icon-image': 'oneway',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor
        // },
        {
            'filter': ['==', ['get', 'barrier'], 'gate'],
            'icon-image': 'gate',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'highway'], 'traffic_signals'],
            'icon-image': 'traffic_light',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        // {
        //     'filter': [
        //         'any',
        //         ['==', ['get', 'railway'], 'level_crossing'],
        //         ['==', ['get', 'railway'], 'crossing']
        //     ],
        //     'icon-image': 'level_crossing2',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor
        // },
        {
            'filter': [
                'any',
                ['==', ['get', 'railway'], 'level_crossing'],
                ['==', ['get', 'railway'], 'crossing']
            ],
            'icon-image': 'level_crossing',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
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
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'barrier'], 'lift_gate'],
                ['==', ['get', 'barrier'], 'swing_gate']
            ],
            'icon-image': 'lift_gate',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'cycle_barrier'],
            'icon-image': 'cycle_barrier',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'stile'],
            'icon-image': 'stile',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        // {
        //     'filter': ['==', ['get', 'highway'], 'mini_roundabout'],
        //     'icon-image': 'highway_mini_roundabout',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor
        // },
        {
            'filter': ['==', ['get', 'barrier'], 'toll_booth'],
            'icon-image': 'toll_booth',
            'icon-color': theme.accommodationIconColor,
            'text-color': theme.accommodationIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'cattle_grid'],
            'icon-image': 'barrier_cattle_grid',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'kissing_gate'],
            'icon-image': 'kissing_gate',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'full-height_turnstile'],
            'icon-image': 'full-height_turnstile',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'barrier'], 'motorcycle_barrier'],
            'icon-image': 'motorcycle_barrier',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'ford'], 'yes'],
                ['==', ['get', 'ford'], 'stepping_stones']
            ],
            'icon-image': 'ford',
            'icon-color': theme.waterIconColor,
            'text-color': theme.waterwayTextColor
        },
        // {
        //     'filter': ['==', ['get', 'mountain_pass'], 'yes'],
        //     'icon-image': 'mountain_pass',
        //     'icon-color': theme.transportationIconColor,
        //     'text-color': theme.transportationIconColor
        // },
        {
            'filter': ['==', ['get', 'waterway'], 'dam'],
            'icon-image': 'place-6',
            'icon-color': theme.waterIconColor,
            'text-color': theme.waterTextColor
        },
        {
            'filter': ['==', ['get', 'waterway'], 'weir'],
            'icon-image': 'place-6',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'waterway'], 'lock_gate'],
            'icon-image': 'place-6',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        // {
        //     'filter': ['==', ['get', 'Node with highway'], 'turning_circle at way with highway'],
        //     'icon-image': 'turning_circle_on_highway_track',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor
        // },

        // Nature
        {
            'filter': ['==', ['get', 'natural'], 'peak'],
            'icon-image': 'peak',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'natural'], 'spring'],
            'icon-image': 'spring',
            'icon-color': theme.waterIconColor,
            'text-color': theme.waterIconColor
        },
        {
            'filter': ['==', ['get', 'natural'], 'cave_entrance'],
            'icon-image': 'cave',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'waterway'], 'waterfall'],
            'icon-image': 'waterfall',
            'icon-color': theme.waterIconColor,
            'text-color': theme.waterIconColor
        },
        {
            'filter': ['==', ['get', 'natural'], 'saddle'],
            'icon-image': 'saddle',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': ['==', ['get', 'natural'], 'volcano'],
            'icon-image': 'peak',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },

        // Administrative facilities
        {
            'filter': ['==', ['get', 'amenity'], 'police'],
            'icon-image': 'police',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'townhall'],
            'icon-image': 'town_hall',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'fire_station'],
            'icon-image': 'firestation',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'social_facility'],
            'icon-image': 'social_facility',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'courthouse'],
            'icon-image': 'courthouse',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'office'], 'diplomatic'],
                ['==', ['get', 'diplomatic'], 'embassy']
            ],
            'icon-image': 'diplomatic',
            'icon-color': theme.officeIconColor,
            'text-color': theme.officeIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'office'], 'diplomatic'],
                ['==', ['get', 'diplomatic'], 'consulate']
            ],
            'icon-image': 'consulate',
            'icon-color': theme.officeIconColor,
            'text-color': theme.officeIconColor
        },
        {
            'filter': ['==', ['get', 'amenity'], 'prison'],
            'icon-image': 'prison',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor
        },

        // Religious place
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'christian']
            ],
            'icon-image': 'christian',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'jewish']
            ],
            'icon-image': 'jewish',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'muslim']
            ],
            'icon-image': 'muslim',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'taoist']
            ],
            'icon-image': 'taoist',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'hindu']
            ],
            'icon-image': 'hinduist',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'buddhist']
            ],
            'icon-image': 'buddhist',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'shinto']
            ],
            'icon-image': 'shintoist',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'amenity'], 'place_of_worship'],
                ['==', ['get', 'religion'], 'sikh']
            ],
            'icon-image': 'sikhist',
            'icon-color': theme.religionIconColor,
            'text-color': theme.religionIconColor
        },
        // {
        //     'filter': [
        //         'all',
        //         ['==', ['get', 'amenity'], 'place_of_worship'],
        //         ['==', ['get', 'without or other religion'], '* value']
        //     ],
        //     'icon-image': 'place_of_worship',
        //     'icon-color': theme.religionIconColor,
        //     'text-color': theme.religionIconColor
        // },

        // Shop and services
        {
            'filter': ['==', ['get', 'amenity'], 'marketplace'],
            'icon-image': 'marketplace',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor
        },
        {
            'filter': ['==', ['get', 'shop'], 'convenience'],
            'icon-image': 'convenience',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'supermarket'],
            'icon-image': 'supermarket',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'clothes'],
                ['==', ['get', 'shop'], 'fashion']
            ],
            'icon-image': 'clothes',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'hairdresser'],
            'icon-image': 'hairdresser',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bakery'],
            'icon-image': 'bakery',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car_repair'],
            'icon-image': 'car_repair',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'doityourself'],
                ['==', ['get', 'shop'], 'hardware']
            ],
            'icon-image': 'diy',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car'],
            'icon-image': 'car',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'kiosk'],
                ['==', ['get', 'shop'], 'newsagent']
            ],
            'icon-image': 'newsagent',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'beauty'],
            'icon-image': 'beauty',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'car_wash'],
            'icon-image': 'car_wash',
            'icon-color': theme.amenityIconColor,
            'text-color': theme.amenityIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'butcher'],
            'icon-image': 'butcher',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['any',
                ['==', ['get', 'shop'], 'alcohol'],
                ['==', ['get', 'shop'], 'wine']
            ],
            'icon-image': 'alcohol',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'furniture'],
            'icon-image': 'furniture',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'florist'],
            'icon-image': 'florist',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'mobile_phone'],
            'icon-image': 'mobile_phone',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'electronics'],
            'icon-image': 'electronics',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'shoes'],
            'icon-image': 'shoes',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'car_parts'],
            'icon-image': 'car_parts',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'greengrocer'],
                ['==', ['get', 'shop'], 'farm']
            ],
            'icon-image': 'greengrocer',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'laundry'],
                ['==', ['get', 'shop'], 'dry_cleaning']
            ],
            'icon-image': 'laundry',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'optician'],
            'icon-image': 'optician',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'jewelry'],
                ['==', ['get', 'shop'], 'jewellery']
            ],
            'icon-image': 'jewelry',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'books'],
            'icon-image': 'library',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'gift'],
            'icon-image': 'gift',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'department_store'],
            'icon-image': 'department_store',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bicycle'],
            'icon-image': 'bicycle',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'confectionery'],
                ['==', ['get', 'shop'], 'chocolate'],
                ['==', ['get', 'shop'], 'pastry']
            ],
            'icon-image': 'confectionery',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'variety_store'],
            'icon-image': 'variety_store',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'travel_agency'],
            'icon-image': 'travel_agency',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'sports'],
            'icon-image': 'sports',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'chemist'],
            'icon-image': 'chemist',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'computer'],
            'icon-image': 'computer',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'stationery'],
            'icon-image': 'stationery',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'pet'],
            'icon-image': 'pet',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'beverages'],
            'icon-image': 'beverages',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'cosmetics'],
                ['==', ['get', 'shop'], 'perfumery']
            ],
            'icon-image': 'perfumery',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tyres'],
            'icon-image': 'tyres',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'motorcycle'],
            'icon-image': 'motorcycle',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'garden_centre'],
            'icon-image': 'garden_centre',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'copyshop'],
            'icon-image': 'copyshop',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'toys'],
            'icon-image': 'toys',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'deli'],
            'icon-image': 'deli',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tobacco'],
            'icon-image': 'tobacco',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'seafood'],
            'icon-image': 'seafood',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'interior_decoration'],
            'icon-image': 'interior_decoration',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'ticket'],
            'icon-image': 'ticket',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'photo'],
                ['==', ['get', 'shop'], 'photo_studio'],
                ['==', ['get', 'shop'], 'photography']
            ],
            'icon-image': 'photo',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'shop'], 'trade'],
                ['==', ['get', 'shop'], 'wholesale']
            ],
            'icon-image': 'trade',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'outdoor'],
            'icon-image': 'outdoor',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'houseware'],
            'icon-image': 'houseware',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'art'],
            'icon-image': 'art',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'paint'],
            'icon-image': 'paint',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'fabric'],
            'icon-image': 'fabric',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bookmaker'],
            'icon-image': 'bookmaker',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'second_hand'],
            'icon-image': 'second_hand',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'charity'],
            'icon-image': 'charity',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bed'],
            'icon-image': 'bed',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'medical_supply'],
            'icon-image': 'medical_supply',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'hifi'],
            'icon-image': 'hifi',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'music'],
            'icon-image': 'music',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'coffee'],
            'icon-image': 'coffee',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'musical_instrument'],
            'icon-image': 'musical_instrument',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'tea'],
            'icon-image': 'tea',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'video'],
            'icon-image': 'video',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'bag'],
            'icon-image': 'bag',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'carpet'],
            'icon-image': 'carpet',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'video_games'],
            'icon-image': 'video_games',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'vehicle_inspection'],
            'icon-image': 'vehicle_inspection',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        {
            'filter': ['==', ['get', 'shop'], 'dairy'],
            'icon-image': 'dairy',
            'icon-color': theme.shopIconColor,
            'text-color': theme.shopIconColor,
        },
        // {
        //     'filter': ['!=', ['get', 'shop'], 'yes'],
        //     'icon-image': 'place-4',
        // 'icon-color': theme.shopIconColor,
        // 'text-color': theme.shopIconColor,
        // },
        {
            'filter': ['==', ['get', 'office'], '*'],
            'icon-image': 'office',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },
        {
            'filter': [
                'any',
                ['==', ['get', 'amenity'], 'nursing_home'],
                ['==', ['get', 'amenity'], 'childcare']
            ],
            'icon-image': 'place-6',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor
        },

        // Landmarks, man-made infrastructure, masts and towers
        {
            'filter': ['any', ['==', ['get', 'man_made'], 'storage_tank'], ['==', ['get', 'man_made'], 'silo']],
            'icon-image': 'storage_tank',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'tower'],
            'icon-image': 'tower_generic',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['all', ['==', ['get', 'man_made'], 'tower'], ['==', ['get', 'tower:type'], 'communication']],
            'icon-image': 'tower_cantilever_communication',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'power'], 'generator'],
                ['any',
                    ['==', ['get', 'generator:source'], 'wind'],
                    ['==', ['get', 'generator:method'], 'wind_turbine)']
                ]
            ],
            'icon-image': 'generator_wind',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'amenity'], 'hunting_stand'],
            'icon-image': 'hunting_stand',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['any', ['==', ['get', 'historic'], 'wayside_cross'], ['==', ['get', 'man_made'], 'cross']],
            'icon-image': 'christian',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'water_tower'],
            'icon-image': 'water_tower',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'mast'],
            'icon-image': 'mast',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            filter: ['==', ['get', 'military'], 'bunker'],
            'icon-image': 'bunker',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'chimney'],
            'icon-image': 'chimney',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
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
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'bell_tower']
            ],
            'icon-image': 'tower_bell_tower',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'lighting']
            ],
            'icon-image': 'tower_lighting',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'lighthouse'],
            'icon-image': 'lighthouse',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'advertising'], 'column'],
            'icon-image': 'column',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'crane'],
            'icon-image': 'crane',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'windmill'],
            'icon-image': 'windmill',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'communication'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice_communication',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'mast'],
                ['==', ['get', 'tower:type'], 'lighting']
            ],
            'icon-image': 'mast_lighting',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'mast'],
                ['==', ['get', 'tower:type'], 'communication']
            ],
            'icon-image': 'mast_communications',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': ['==', ['get', 'man_made'], 'communications_tower'],
            'icon-image': 'communication_tower',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'defensive']
            ],
            'icon-image': 'tower_defensive',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'cooling']
            ],
            'icon-image': 'tower_cooling',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:type'], 'lighting'],
                ['==', ['get', 'tower:construction'], 'lattice']
            ],
            'icon-image': 'tower_lattice_lighting',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'dish']
            ],
            'icon-image': 'tower_dish',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'tower'],
                ['==', ['get', 'tower:construction'], 'dome']
            ],
            'icon-image': 'tower_dome',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'telescope'],
                ['==', ['get', 'telescope:type'], 'radio']
            ],
            'icon-image': 'telescope_dish',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },
        {
            'filter': [
                'all',
                ['==', ['get', 'man_made'], 'telescope'],
                ['==', ['get', 'telescope:type'], 'optical']
            ],
            'icon-image': 'telescope_dome',
            'icon-color': theme.manMadeIconColor,
            'text-color': theme.manMadeIconColor,
        },

        // Electricity
        // {
        //     'filter': ['==', ['get', 'power'], 'tower'],
        //     'icon-image': 'power_tower',
        //     'icon-color': theme.powerIconColor,
        //     'text-color': theme.powerIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'power'], 'pole'],
        //     'icon-image': 'place-6',
        //     'icon-color': theme.powerIconColor,
        //     'text-color': theme.powerIconColor,
        // },

        // Places
        {
            'filter': ['==', ['get', 'place'], 'city'],
            'icon-image': 'place-6',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor,
        },
        // {
        //     'filter': ['has', 'capital'],
        //     'icon-image': 'place_capital',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'yes'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'main'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor,
        // },
        // {
        //     'filter': ['==', ['get', 'entrance'], 'service'],
        //     'icon-image': 'entrance',
        //     'icon-color': theme.defaultIconColor,
        //     'text-color': theme.defaultIconColor,
        // },
        {
            'filter': [
                'all',
                ['has', 'entrance'],
                ['==', ['get', 'access'], 'no']
            ],
            'icon-image': 'entrance',
            'icon-color': theme.defaultIconColor,
            'text-color': theme.defaultIconColor,
        },
    ]),
});
