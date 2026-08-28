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
        filter: ['==', ['get', 'landuse'], 'village_green'],
        'fill-color': theme.landuseVillageGreenBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'salt_pond'],
        'fill-color': theme.landuseSaltPondBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'religious'],
        'fill-color': theme.landuseReligiousBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'recreation_ground'],
        'fill-color': theme.landuseRecreationGroundBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'railway'],
        'fill-color': theme.landuseRailwayBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'quarry'],
        'fill-color': theme.landuseQuarryBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'plant_nursery'],
        'fill-color': theme.landusePlantNurseryBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'landfill'],
        'fill-color': theme.landuseLandfillBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'greenfield'],
        'fill-color': theme.landuseGreenfieldBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'garages'],
        'fill-color': theme.landuseGaragesBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'cemetery'],
        'fill-color': theme.landuseCemeteryBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'brownfield'],
        'fill-color': theme.landuseBrowmfieldBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'basin'],
        'fill-color': theme.landuseBasinBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'vineyard'],
        'fill-color': theme.landuseVineyardBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'farmyard'],
        'fill-color': theme.landuseFarmyardBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'farmland'],
        'fill-color': theme.landuseFarmlandBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'allotments'],
        'fill-color': theme.landuseAllotmentsBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'retail'],
        'fill-color': theme.landuseRetailBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'industrial'],
        'fill-color': theme.landuseIndustrialBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'residential'],
        'fill-color': theme.landuseResidentialBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'construction'],
        'fill-color': theme.landuseConstructionBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'commercial'],
        'fill-color': theme.landuseCommercialBackgroundFillColor,
    },
    {
        filter: ['==', ['get', 'landuse'], 'pedestrian'],
        'fill-color': theme.landusePedestrianBackgroundFillColor,
    },
];

export default asLayerObject(withSortKeys(directives), {
    id: 'landuse_background',
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
