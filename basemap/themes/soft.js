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

/**
 * A quiet basemap in the manner of the mobile maps of the last decade, Apple Maps above all: warm
 * off-white ground, soft blue water, muted greens, white roads over light casings, and labels doing
 * most of the work of telling one thing from another.
 *
 * Every other theme in this directory is the default one put through a transform of each colour,
 * which is what those themes are: a map inverted, drained of colour, or seen through a colour
 * vision deficiency. This one is a palette. No transform reaches it, because the difference is not
 * in the colours but in what the map chooses to colour at all. OpenStreetMap Carto, which the
 * default theme follows, gives every land use a hue of its own, and a motorway is pink because
 * pink is the colour that class of road has been drawn in for a century of paper maps. A map made
 * for a screen you hold in one hand keeps hue for the few things a reader looks for and flattens
 * everything else towards the ground, so that a label or a route laid over it stays the loudest
 * thing on the screen.
 *
 * It states every colour the default theme states, so that the two differ only where they mean to
 * and a value never arrives here by accident. The spread underneath is the fallback for a key added
 * to the default theme later: it will appear here in its default colour, which validate.js reports
 * as a theme key that this file does not name.
 */
import defaultTheme from './default.js';

// Sampled from Apple Maps rather than guessed at, which is what made the first attempt at this
// palette too quiet: a screen map is not a drained map. It keeps its saturation and spends it on
// three things, the water, the open country and the pins, and takes it away from everything else.

// The ground, and the surfaces that sit directly on it, all within a few steps of each other so
// that nothing on the ground competes with what is drawn over it. This is what a built-up area
// comes out as, the buildings and roads of a town reading against it rather than colouring it.
const land = 'rgb(243, 240, 230)';
const landDim = 'rgb(238, 236, 228)';
const landInstitution = 'rgb(241, 238, 228)';
// A building is neutral where the ground it stands on is warm, which is what separates the two:
// they are within a few steps of each other in lightness, and a building drawn warm at that
// distance sinks into the ground rather than sitting on it. What draws the shape is the edge,
// which is darker than either by a wide margin.
const building = 'rgb(230, 229, 227)';
const buildingOutline = 'rgb(188, 186, 184)';
// A building on land that is worked, traded on or farmed, one step off the neutral above and
// no further. These are read in quantity and against each other, so what tells them apart is
// which way each leans, warm for trade, cool for industry, earth for farming, and not how dark
// it is: a building that darkens with its zoning reads as a taller building.
const buildingCommercial = 'rgb(234, 226, 216)';
const buildingIndustrial = 'rgb(224, 227, 230)';
const buildingAgricultural = 'rgb(232, 226, 211)';

// Water is the most saturated thing on the map, being the thing a reader finds first.
const water = 'rgb(159, 218, 244)';
const waterEdge = 'rgb(124, 198, 233)';
const waterInk = 'rgb(64, 143, 196)';

// Four greens, yellow rather than blue, and told apart by depth: open country is the palest and
// the most of what a reader sees outside a town, tended ground sits above it, and tree cover is
// the deepest. Between them they are what makes the countryside read as countryside.
const greenField = 'rgb(221, 231, 179)';
const green = 'rgb(198, 228, 164)';
const greenDeep = 'rgb(190, 222, 158)';
const greenEdge = 'rgb(176, 210, 144)';
const greenInk = 'rgb(94, 134, 82)';

const sand = 'rgb(241, 234, 215)';
const rock = 'rgb(230, 228, 220)';
const ice = 'rgb(226, 238, 244)';
const earth = 'rgb(198, 184, 152)';

// The road hierarchy, which is a single ramp of neutral greys from white up to the motorway. No
// class carries a hue: what ranks a road is how dark it is, the motorway being the darkest thing
// drawn on the ground and an ordinary street the lightest. A paper map ranks roads by colour
// because colour is what it has; a screen map has a reader holding it at arm's length, and a ramp
// of one hue is read at a glance where a set of hues has to be learnt. The values are measured off
// Apple Maps rather than chosen: white 247, main roads 222, then 201, then the motorway at 179.
const road = 'rgb(247, 248, 248)';
const roadCasing = 'rgb(222, 224, 227)';
const roadCasingMinor = 'rgb(232, 234, 236)';
const roadTunnel = 'rgb(250, 250, 250)';
const roadTunnelCasing = 'rgb(236, 238, 240)';
const tertiary = 'rgb(228, 230, 233)';
const tertiaryCasing = 'rgb(206, 208, 213)';
const tertiaryTunnel = 'rgb(237, 239, 241)';
const secondary = 'rgb(213, 215, 219)';
const secondaryCasing = 'rgb(194, 196, 201)';
const secondaryTunnel = 'rgb(226, 228, 231)';
const primary = 'rgb(201, 203, 206)';
const primaryCasing = 'rgb(182, 184, 190)';
const primaryTunnel = 'rgb(217, 219, 222)';
const trunk = 'rgb(190, 192, 197)';
const trunkCasing = 'rgb(171, 173, 179)';
const trunkTunnel = 'rgb(209, 211, 215)';
const motorway = 'rgb(179, 181, 186)';
const motorwayCasing = 'rgb(160, 162, 168)';
const motorwayTunnel = 'rgb(200, 202, 207)';
const rail = 'rgb(186, 188, 192)';

// Labels carry the map, so they are neutral and they descend by weight, not by hue. They are a
// grey and not a black: a label darker than this stops reading as part of the map and starts
// reading as something laid on top of it.
const ink = 'rgb(66, 68, 68)';
const inkMuted = 'rgb(91, 93, 93)';
const inkFaint = 'rgb(134, 136, 136)';
const halo = 'rgba(255, 255, 255, 0.9)';

// Points of interest are where the rest of the saturation goes: a handful of strong hues, each
// standing for a category a reader is looking for by name.
const iconOrange = 'rgb(240, 145, 45)';
const iconBlue = 'rgb(58, 140, 220)';
const iconGreen = 'rgb(72, 159, 131)';
const iconRed = 'rgb(230, 80, 80)';
const iconViolet = 'rgb(150, 110, 210)';
const iconMagenta = 'rgb(205, 95, 175)';
const iconNeutral = 'rgb(126, 124, 118)';

export default {
    ...defaultTheme,

    accommodationIconColor: iconViolet,
    aerialwayLineColor: 'rgb(168, 170, 174)',
    aerowayPolygonColor: 'rgb(236, 235, 231)',
    aerowayRunwayLineColor: 'rgb(222, 224, 227)',
    aerowayTaxiwayLineColor: 'rgb(230, 232, 234)',
    amenityCollegeBackgroundFillColor: landInstitution,
    amenityFountainFillColor: water,
    amenityFountainFillOutlineColor: waterEdge,
    amenityGraveYardBackgroundFillColor: 'rgb(210, 226, 176)',
    amenityHospitalBackgroundFillColor: 'rgb(243, 237, 231)',
    amenityIconColor: iconNeutral,
    amenityKinderGartenFillColor: landInstitution,
    amenityParkingOverlayFillColor: 'rgb(236, 234, 226)',
    amenityParkingOverlayOutlineColor: 'rgb(224, 222, 214)',
    amenityPublicBathIconColor: iconBlue,
    amenityPublicBathTextColor: inkMuted,
    amenitySchoolFillColor: landInstitution,
    amenityUniversityBackgroundFillColor: landInstitution,
    attractionWaterSlideLineColor: waterEdge,
    backgroundColor: land,
    barrierCityWallLineColor: 'rgb(202, 204, 208)',
    barrierFenceLineColor: 'rgb(214, 216, 219)',
    barrierGuardRailBackgroundLineColor: 'rgb(210, 212, 216)',
    barrierHedgeLineColor: greenDeep,
    barrierWallLineColor: 'rgb(206, 208, 212)',
    boundaryAdminLevelLineColor: 'rgb(192, 186, 196)',
    buildingAgriculturalFillColor: buildingAgricultural,
    buildingCemeteryTextColor: greenInk,
    buildingCommercialFillColor: buildingCommercial,
    buildingFillColor: building,
    buildingIndustrialFillColor: buildingIndustrial,
    buildingNumberTextColor: 'rgb(152, 150, 144)',
    buildingNumberTextHaloColor: halo,
    buildingOutlineColor: buildingOutline,
    defaultIconColor: iconNeutral,
    gastronomyIconColor: iconOrange,
    healthIconColor: iconRed,
    highwayBuswayDashColor: iconBlue,
    highwayBuswayLineColor: road,
    highwayBuswayOutlineColor: roadCasing,
    highwayDefaultConstructionLineColor: 'rgb(212, 214, 218)',
    highwayPathLineColor: earth,
    highwayLabelColor: inkMuted,
    highwayLabelHaloColor: halo,
    highwayLineUnclassifiedBridgeLineColor: road,
    highwayLivingStreetBridgeLineColor: road,
    highwayLivingStreetBridgeOutlineColor: roadCasing,
    highwayLivingStreetLineColor: road,
    highwayLivingStreetOutlineColor: roadCasingMinor,
    highwayLivingStreetTunnelLineColor: roadTunnel,
    highwayLivingStreetTunnelOutlineColor: roadTunnelCasing,
    highwayMotorwayBridgeLineColor: motorway,
    highwayMotorwayBridgeOutlineColor: 'rgb(166, 168, 174)',
    highwayMotorwayLineColor: motorway,
    highwayMotorwayOutlineColor: motorwayCasing,
    highwayMotorwayTunnelLineColor: motorwayTunnel,
    highwayMotorwayTunnelOutlineColor: 'rgb(198, 200, 205)',
    highwayOutlinePedestrianBridgeLineColor: 'rgb(210, 212, 216)',
    highwayPedestrianBridgeLineColor: 'rgb(240, 239, 234)',
    highwayPedestrianLineColor: 'rgb(242, 240, 234)',
    highwayPedestrianOutlineColor: 'rgb(226, 227, 230)',
    highwayPedestrianTunnelLineColor: 'rgb(246, 245, 241)',
    highwayPedestrianTunnelOutlineColor: 'rgb(233, 234, 237)',
    highwayPrimaryBridgeLineColor: primary,
    highwayPrimaryBridgeOutlineColor: 'rgb(172, 174, 180)',
    highwayPrimaryLineColor: primary,
    highwayPrimaryOutlineColor: primaryCasing,
    highwayPrimaryTunnelLineColor: primaryTunnel,
    highwayPrimaryTunnelOutlineColor: 'rgb(196, 198, 203)',
    highwayRacewayBridgeLineColor: 'rgb(238, 200, 204)',
    highwayRacewayLineColor: 'rgb(244, 216, 219)',
    highwayRacewayTunnelLineColor: 'rgb(248, 232, 234)',
    highwayResidentialBridgeLineColor: road,
    highwayResidentialBridgeOutlineColor: roadCasing,
    highwayResidentialLineColor: road,
    highwayResidentialOutlineColor: roadCasingMinor,
    highwayResidentialTunnelLineColor: roadTunnel,
    highwayResidentialTunnelOutlineColor: roadTunnelCasing,
    highwaySecondaryBridgeLineColor: secondary,
    highwaySecondaryBridgeOutlineColor: 'rgb(184, 186, 192)',
    highwaySecondaryLineColor: secondary,
    highwaySecondaryOutlineColor: secondaryCasing,
    highwaySecondaryTunnelLineColor: secondaryTunnel,
    highwaySecondaryTunnelOutlineColor: 'rgb(208, 210, 215)',
    highwayServiceBridgeLineColor: road,
    highwayServiceBridgeOutlineColor: roadCasingMinor,
    highwayServiceLineColor: road,
    highwayServiceOutlineColor: 'rgb(228, 230, 233)',
    highwayServiceTunnelLineColor: roadTunnel,
    highwayServiceTunnelOutlineColor: 'rgb(236, 238, 240)',
    highwayTertiaryBridgeLineColor: tertiary,
    highwayTertiaryBridgeOutlineColor: 'rgb(196, 198, 203)',
    highwayTertiaryLineColor: tertiary,
    highwayTertiaryOutlineColor: tertiaryCasing,
    highwayTertiaryTunnelLineColor: tertiaryTunnel,
    highwayTertiaryTunnelOutlineColor: 'rgb(218, 220, 224)',
    highwayTrackBridgeLineColor: 'rgb(186, 170, 138)',
    highwayTrackLineColor: 'rgb(214, 200, 170)',
    highwayTrackTunnelLineColor: 'rgb(226, 214, 190)',
    highwayTrunkBridgeLineColor: trunk,
    highwayTrunkBridgeOutlineColor: 'rgb(178, 181, 187)',
    highwayTrunkLineColor: trunk,
    highwayTrunkOutlineColor: trunkCasing,
    highwayTrunkTunnelLineColor: trunkTunnel,
    highwayTrunkTunnelOutlineColor: 'rgb(206, 209, 214)',
    highwayUnclassifiedBridgeOutlineColor: roadCasing,
    highwayUnclassifiedLineColor: road,
    highwayUnclassifiedOutlineColor: roadCasingMinor,
    highwayUnclassifiedTunnelLineColor: roadTunnel,
    highwayUnclassifiedTunnelOutlineColor: roadTunnelCasing,
    historyIconColor: iconNeutral,
    landuseAllotmentsBackgroundFillColor: green,
    landuseBasinBackgroundFillColor: water,
    landuseBrowmfieldBackgroundFillColor: 'rgb(230, 228, 216)',
    landuseCemeteryBackgroundFillColor: 'rgb(210, 226, 176)',
    landuseCommercialBackgroundFillColor: 'rgb(242, 239, 229)',
    landuseConstructionBackgroundFillColor: 'rgb(235, 233, 223)',
    landuseFarmlandBackgroundFillColor: greenField,
    landuseFarmyardBackgroundFillColor: 'rgb(238, 231, 208)',
    landuseForestOverlayFillColor: greenDeep,
    landuseForestTextColor: greenInk,
    landuseGaragesBackgroundFillColor: landDim,
    landuseGrassOverlayFillColor: green,
    landuseGreenfieldBackgroundFillColor: greenField,
    landuseGreenhouseHorticultureOverlayFillColor: greenField,
    landuseIndustrialBackgroundFillColor: landDim,
    landuseIndustrialTextColor: 'rgb(130, 128, 122)',
    landuseLandfillBackgroundFillColor: 'rgb(230, 228, 216)',
    landuseMeadowOverlayFillColor: green,
    landuseMilitaryOverlayFillColor: 'rgba(226, 172, 166, 0.18)',
    landuseOrchardOverlayFillColor: greenDeep,
    landusePedestrianBackgroundFillColor: 'rgb(240, 238, 230)',
    landusePlantNurseryBackgroundFillColor: greenDeep,
    landuseQuarryBackgroundFillColor: 'rgb(228, 226, 216)',
    landuseQuarryTextColor: 'rgb(120, 118, 112)',
    landuseRailwayBackgroundFillColor: landDim,
    landuseRecreationGroundBackgroundFillColor: green,
    landuseReligiousBackgroundFillColor: 'rgb(237, 234, 224)',
    landuseResidentialBackgroundFillColor: 'rgb(241, 238, 228)',
    landuseRetailBackgroundFillColor: 'rgb(243, 238, 228)',
    landuseSaltPondBackgroundFillColor: water,
    landuseVillageGreenBackgroundFillColor: green,
    landuseVineyardBackgroundFillColor: greenDeep,
    leisureDogParkOverlayFillColor: green,
    leisureFitnessStationOverlayFillColor: green,
    leisureFitnessStationOverlayOutlineColor: greenEdge,
    leisureGardenBackgroundFillColor: green,
    leisureGolfCourseBackgroundFillColor: green,
    leisureIceRinkOverlayFillColor: ice,
    leisureIceRinkOverlayOutlineColor: 'rgb(198, 222, 232)',
    leisureIconColor: iconGreen,
    leisureMarinaOverlayFillColor: 'rgb(204, 230, 244)',
    leisureMarinaOverlayOutlineColor: waterEdge,
    leisureMiniatureGolfOverlayFillColor: greenDeep,
    leisureNatureReserveLineColor: 'rgba(150, 196, 128, 0.6)',
    leisureParkBackgroundFillColor: green,
    leisureParkTextColor: greenInk,
    leisurePitchOverlayFillColor: 'rgb(186, 220, 152)',
    leisurePitchOverlayOutlineColor: greenEdge,
    leisurePitchTextColor: greenInk,
    leisurePlayGroundOverlayFillColor: green,
    leisurePlayGroundOverlayOutlineColor: greenEdge,
    leisureSportsCentreBackgroundFillColor: green,
    leisureStadiumOverlayFillColor: green,
    leisureStadiumTextColor: greenInk,
    leisureSwimmingPoolBackgroundFillColor: water,
    leisureSwimmingPoolOverlayOutlineColor: waterEdge,
    leisureTrackBackgroundFillColor: green,
    leisureTrackBackgroundFillOutlineColor: greenEdge,
    leisureTrackLineColor: green,
    manMadeBridgeFillColor: 'rgb(226, 228, 231)',
    manMadeGroyneFillColor: 'rgb(222, 224, 227)',
    manMadeIconColor: iconNeutral,
    manMadePierFillColor: 'rgb(226, 228, 231)',
    manMadePierLineColor: land,
    manMadePierTextColor: inkMuted,
    manMadePierTextHaloColor: halo,
    manMadeWasteWaterPlantFillColor: landDim,
    naturalBareRockBackgroundFillColor: rock,
    naturalBeachGravelOverlayFillColor: 'rgb(236, 228, 204)',
    naturalBeachOverlayFillColor: sand,
    naturalCliffLineColor: 'rgb(200, 198, 190)',
    naturalGlacierBackgroundFillColor: ice,
    naturalGlacierTextColor: 'rgb(118, 148, 164)',
    naturalGrasslandBackgroundFillColor: green,
    naturalHeathBackgroundFillColor: 'rgb(214, 224, 168)',
    naturalPeakIconColor: 'rgb(138, 126, 110)',
    naturalPeakTextColor: 'rgb(116, 104, 90)',
    naturalSandOverlayFillColor: sand,
    naturalScreeBackgroundFillColor: rock,
    naturalScrubOverlayFillColor: 'rgb(206, 228, 172)',
    naturalShingleBackgroundFillColor: rock,
    naturalTreeCircleColor: 'rgb(150, 200, 132)',
    naturalTreeRowLineColor: greenDeep,
    naturalTrunkCircleColor: earth,
    naturalWaterFillColor: water,
    naturalWetlandOverlayFillColor: 'rgb(196, 228, 206)',
    naturalWoodBackgroundFillColor: greenDeep,
    oceanWaterFillColor: water,
    officeIconColor: 'rgb(88, 120, 178)',
    pedestrianAreaFillColor: 'rgb(240, 238, 230)',
    placeCityTextColor: ink,
    placeCountryTextColor: 'rgb(84, 86, 86)',
    placeCountryTextHaloColor: halo,
    placeLocalityTextColor: inkFaint,
    placeRegionTextColor: 'rgb(102, 104, 104)',
    placeSuburbTextColor: 'rgb(112, 114, 114)',
    placeTextHaloColor: halo,
    placeTownTextColor: 'rgb(78, 80, 80)',
    placeVillageTextColor: 'rgb(96, 98, 98)',
    pointIconHaloColor: halo,
    pointTextHaloColor: halo,
    powerCableLineColor: 'rgb(212, 214, 218)',
    powerIconColor: iconNeutral,
    powerPlantBackgroundFillColor: landDim,
    powerPlantBackgroundOutlineColor: 'rgb(218, 217, 210)',
    powerTowerCircleColor: 'rgb(188, 190, 194)',
    railwayLineColor: rail,
    railwayMinorLineColor: 'rgb(210, 212, 216)',
    railwayTunnelColor: 'rgb(220, 222, 226)',
    railwayUrbanLineColor: 'rgb(200, 202, 206)',
    religionIconColor: iconNeutral,
    routeFerryLineColor: 'rgb(122, 178, 216)',
    shopIconColor: iconMagenta,
    terrainContourIndexLineColor: 'rgba(186, 172, 138, 0.6)',
    terrainContourLineColor: 'rgba(198, 186, 156, 0.45)',
    terrainContourTextColor: 'rgba(152, 138, 110, 1)',
    terrainContourTextHaloColor: 'rgba(255, 255, 255, 0.9)',
    tourismAttractionFillColor: 'rgb(238, 232, 216)',
    tourismCampSiteFillColor: green,
    transportDefaultIconColor: iconViolet,
    transportationIconColor: iconBlue,
    waterIconColor: waterInk,
    waterTextColor: waterInk,
    waterwayLineColor: water,
    waterwayTextColor: waterInk,
    waterwayTextHaloColor: halo,
    waterwayTunnelColor: 'rgb(200, 230, 246)',
};
