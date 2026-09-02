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
 * The names of roads, brought in by class rather than all at once.
 *
 * Every named highway carried a label from zoom 14, and in a town that is every footway, every
 * driveway and every set of steps competing with the streets around them: the name a reader is
 * looking for is somewhere in the field, and the labels that beat it to the space are the ones for
 * paths between two buildings. So the classes arrive over three zooms, widest first, and each zoom
 * adds the next rank to the ones already placed.
 *
 * The layer still starts at 14. What a zoom draws decides what its tiles carry, and reaching lower
 * for the motorways would put a name on every highway in the tiles at 12 and 13 to label the few.
 */
const MAJOR = ['motorway', 'motorway_link', 'trunk', 'trunk_link',
    'primary', 'primary_link', 'secondary', 'secondary_link'];

const MINOR = ['tertiary', 'tertiary_link', 'unclassified', 'residential', 'living_street',
    'pedestrian', 'busway', 'raceway'];

const REST = ['service', 'track', 'path', 'footway', 'cycleway', 'bridleway', 'steps',
    'sidewalk', 'crossing', 'road'];

const drawn = (classes) => ['match', ['get', 'highway'], classes, true, false];

export default {
    id: 'highway_label',
    type: 'symbol',
    sourceLayer: 'highway',
    minzoom: 14,
    filter: [
        'all',
        ['has', 'name'],
        [
            'step', ['zoom'],
            drawn(MAJOR),
            15, drawn([...MAJOR, ...MINOR]),
            16, drawn([...MAJOR, ...MINOR, ...REST]),
        ],
    ],
    layout: {
        'symbol-placement': 'line',
        'text-anchor': 'center',
        'text-field': ['get', 'name'],
        'text-font': ['Noto Sans Regular'],
        'text-size': 11,
        // A name follows the road it names, and a road bends. Past this angle between two
        // characters the label is dropped rather than drawn around the corner, which is what the
        // default of 45 degrees lets it do on a hairpin.
        'text-max-angle': 30,
    },
    paint: {
        'text-color': theme.highwayLabelColor,
        'text-halo-color': theme.highwayLabelHaloColor,
        'text-halo-width': 1.2,
    },
}
