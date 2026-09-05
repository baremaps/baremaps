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
 * Which way a one-way street runs, as arrows set along it.
 *
 * From zoom 16, which is where this map becomes a street map. Every road width in the theme tops
 * out at 16, the names of the minor streets arrive at 16, and the points arrive at 16; below that
 * the map is a map of a district, and the direction of a particular street is not a thing a reader
 * of a district is asking. Two zooms lower it would also be unreadable and in the way: a
 * residential street is 2.5 pixels wide at 14 against 3 at 16, and 14 and 15 are the zooms where
 * the road names are being placed. An arrow and a name want the same piece of a line, and the name
 * is what a reader is hunting for.
 *
 * The tiles agree. The attributes a tile carries are the ones the style reads at that zoom, so
 * drawing from 16 puts `oneway` in the z16 tiles and no others; the source stops at 16, so that
 * same tile serves every zoom past it and the arrows go on working to the bottom of the map for
 * nothing further. It is not a large sum either way -- on the z16 tile over the old town of Zurich
 * the tag costs 86 bytes gzipped against 8.8 KB of highways, and reaching down to 14 would spend
 * a little over twice that across the three levels it would then be carried in -- which is why the
 * zoom is chosen on what the reader can use rather than on the bytes.
 *
 * One layer covers the road, its bridges and its tunnels, where the line and the outline need
 * three apiece. Those three exist because a casing has to be painted under the roads around it and
 * over the ground beneath it, and a bridge and a tunnel sit at different heights in that stack; an
 * arrow is drawn on top of the finished roadway whichever of the three carries it.
 */

/** The values of `oneway` that mean the way is one-way, and those that reverse it. */
const FORWARD = ['yes', '1', 'true'];
const BACKWARD = ['-1', 'reverse'];

export default {
    id: 'highway_oneway',
    type: 'symbol',
    sourceLayer: 'highway',
    minzoom: 16,
    filter: [
        'all',
        ['==', ['geometry-type'], 'LineString'],
        ['in', ['get', 'oneway'], ['literal', [...FORWARD, ...BACKWARD]]],
    ],
    layout: {
        'symbol-placement': 'line',
        // The arrows are spaced closer than the 250 pixels a symbol layer spaces itself at, which
        // is not a matter of taste: a line shorter than half the spacing gets no anchor at all,
        // and half the one-way ways in the extract are under 57 pixels long at zoom 16. At 250 the
        // arrows would land on the avenues and miss the one-way lanes of an old town, which are
        // the streets the reader cannot guess. This is about one arrow per city block.
        'symbol-spacing': 120,
        // An arrow rather than a rotation. A line-placed symbol is already turned to follow its
        // line, so the direction is said by which of the two glyphs is set, and `oneway=-1` is a
        // way drawn against its own direction of travel rather than a separate case to style.
        'text-field': ['match', ['get', 'oneway'], BACKWARD, '←', '→'],
        'text-font': ['Noto Sans Regular'],
        // Set large for a label, because the glyph is mostly air: the arrow inks 0.56 of its em
        // where a capital fills 0.71 and reaches the full width of its advance, so it draws about
        // 12 pixels of arrow here, against the 8 a name of this size gives its capitals.
        'text-size': 22,
        // The arrow is carried up onto the middle of the road it marks. A line-placed label is
        // centred by its line box, which suits letters sitting between the baseline and the cap
        // line; this glyph is drawn on the mathematical axis, two thirds of an em up, and the same
        // centring leaves it a fifth of an em low -- measured on a 24 pixel road at zoom 19, where
        // it sat 7 pixels under the centre line. Ems, so it holds at every size.
        'text-offset': [0, -0.2],
        // Text set along a line is turned upright when the line runs right to left, so that a name
        // is never read upside down. An arrow is not read, it is pointed: flipping it says the
        // opposite of what the data says, and on exactly the half of the streets that run westward.
        'text-keep-upright': false,
    },
    paint: {
        // Lighter than a street name, and haloed like one. The arrow is a mark on a road the
        // reader has already found, so it has to be legible over a white residential street and a
        // pink motorway alike without taking the eye off the names above it.
        'text-color': theme.highwayOnewayTextColor,
        'text-halo-color': theme.highwayOnewayTextHaloColor,
        'text-halo-width': 1.5,
    },
}
