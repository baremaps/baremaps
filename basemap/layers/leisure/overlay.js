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
    id: 'leisure_overlay',
    type: 'fill',
    sourceLayer: 'leisure',
    layout: {
        visibility: 'visible',
    },
    paint: {
        'fill-antialias': false,
    },
    filter: ['==', ["geometry-type"], 'Polygon'],
    directives: withSortKeys([
        {
            filter: ['==', ['get', 'leisure'], 'marina'],
            'fill-color': theme.leisureMarinaOverlayFillColor,
            'fill-outline-color': theme.leisureMarinaOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'swimming_pool'],
            'fill-color': theme.leisureSwimmingPoolBackgroundFillColor,
            'fill-outline-color': theme.leisureSwimmingPoolOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'fitness_station'],
            'fill-color': theme.leisureFitnessStationOverlayFillColor,
            'fill-outline-color': theme.leisureFitnessStationOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'miniature_golf'],
            'fill-color': theme.leisureMiniatureGolfOverlayFillColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'ice_rink'],
            'fill-color': theme.leisureIceRinkOverlayFillColor,
            'fill-outline-color': theme.leisureIceRinkOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'dog_park'],
            'fill-color': theme.leisureDogParkOverlayFillColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'playground'],
            'fill-color': theme.leisurePlayGroundOverlayFillColor,
            'fill-outline-color': theme.leisurePlayGroundOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'pitch'],
            'fill-color': theme.leisurePitchOverlayFillColor,
            'fill-outline-color': theme.leisurePitchOverlayOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'track'],
            'fill-color': theme.leisureTrackBackgroundFillColor,
            'fill-outline-color': theme.leisureTrackBackgroundFillOutlineColor,
        },
        {
            filter: ['==', ['get', 'leisure'], 'stadium'],
            'fill-color': theme.leisureStadiumOverlayFillColor,
        },
    ]),
});
