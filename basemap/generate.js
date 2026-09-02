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
 * Writes out the eight themes drawn for a reader with a colour vision deficiency.
 *
 * Run with `node generate.js`, or `npm run themes`. `npm test` reports the files on disk drifting
 * from what this writes, which is what happens when `themes/default.js` gains a colour or moves one.
 *
 * They are written out rather than worked out on load because Baremaps evaluates the style through
 * GraalJS, which interprets it. Measuring a palette of this size against a model of an eye, and
 * settling the colours that collide, is a second of that interpreter's time per theme and about ten
 * for the eight; the answer only changes when the default palette does, so it is paid here instead.
 * Every other theme in `themes/` is a transform of a colour at a time, which costs nothing worth
 * moving.
 */
import {writeFileSync, readFileSync} from 'fs';
import {fileURLToPath} from 'url';
import {join, dirname} from 'path';

import defaultTheme from './themes/default.js';
import {adaptPalette, lostDistinctions, DICHROMACY, ANOMALY} from './utils/vision.js';

const root = dirname(fileURLToPath(import.meta.url));
const LICENSE = readFileSync(join(root, 'themes', 'default.js'), 'utf8').split(' **/')[0] + ' **/';

/**
 * What each theme is: the eye it is drawn for, and how far the colours that collide for that eye
 * are pushed apart in lightness.
 *
 * The distance is not searched for on every build. `utils/vision.js` offers four, each was measured
 * against this palette and this eye, and the one that leaves the reader with the fewest distinctions
 * lost is named here. Protanopia is the exception and says why.
 */
const THEMES = {
    protanopia: {
        type: 'protan', severity: DICHROMACY, severityName: 'DICHROMACY', separation: 2.5,
        cone: 'long-wavelength cone does not work',
        note: 'A wider push scores better on the palette as a whole -- six leaves 120 rather than\n'
            + ' * 124 -- and pays for it in the labels: it moves the ground and the halo as readily as the\n'
            + ' * fills, and four label colours came out under the contrast text needs against a halo that\n'
            + ' * had shifted beneath them. A distinction a reader cannot see costs less than a name they\n'
            + ' * cannot read.',
    },
    protanomaly: {
        type: 'protan', severity: ANOMALY, severityName: 'ANOMALY', separation: 2.5,
        cone: 'long-wavelength cone answers to light nearer the middle of the spectrum than it should',
    },
    deuteranopia: {
        type: 'deutan', severity: DICHROMACY, severityName: 'DICHROMACY', separation: 2.5,
        cone: 'medium-wavelength cone does not work',
    },
    deuteranomaly: {
        type: 'deutan', severity: ANOMALY, severityName: 'ANOMALY', separation: 4,
        cone: 'medium-wavelength cone answers to light nearer the long end of the spectrum than it should',
    },
    tritanopia: {
        type: 'tritan', severity: DICHROMACY, severityName: 'DICHROMACY', separation: 4,
        cone: 'short-wavelength cone does not work',
    },
    tritanomaly: {
        type: 'tritan', severity: ANOMALY, severityName: 'ANOMALY', separation: 2.5,
        cone: 'short-wavelength cone answers to light nearer the middle of the spectrum than it should',
    },
    achromatopsia: {
        type: 'achromat', severity: DICHROMACY, severityName: 'DICHROMACY',
        reads: 'luminance and no hue at all',
    },
    achromatomaly: {
        type: 'achromat', severity: ANOMALY, severityName: 'ANOMALY',
        reads: 'far less hue than a full set of cones gives',
    },
};

const DICHROMAT_PROSE = (name, spec, before, after) => `/**
 * The map drawn for a reader with ${name}, whose ${spec.cone}.
 *
 * Not the map as that reader sees it. A simulation handed to the reader it depicts applies the
 * deficiency twice, once by the palette and once by the eye. This is the default palette with the
 * collisions taken out of it: every pair of colours that palette draws far enough apart to be
 * saying different things is checked against what this eye makes of them, and the pairs that arrive
 * at the same colour are moved apart in lightness, which is the one thing such an eye reads as well
 * as any other.
 *
 * It takes the distinctions this reader loses from ${before} to ${after}.${spec.note ? '\n *\n * ' + spec.note : ''}
 *
 * Generated, and not to be edited: \`npm run themes\` writes it from \`themes/default.js\` through
 * \`adaptPalette(palette, '${spec.type}', ${spec.severityName}, [${spec.separation}])\` in \`utils/vision.js\`, and
 * \`npm test\` reports it drifting from what that produces. See \`generate.js\` for why it is written
 * out rather than worked out when the style is built.
 */`;

const ACHROMAT_PROSE = (name, spec) => `/**
 * The map drawn for a reader with ${name}, who reads ${spec.reads}.
 *
 * Every colour becomes the grey of its own luminance, and nothing further can be done for this
 * reader by moving colours around: the other six themes recover a lost distinction by moving it into
 * lightness, and lightness is the channel this reader is already down to. Two classes of the same
 * lightness stay one class.
 *
 * What this reader needs is a palette whose classes are far enough apart in lightness to begin with,
 * which is something to draw rather than something to compute, and a map that carries what lightness
 * cannot in size, in weight and in the label itself.
 *
 * Generated, and not to be edited: \`npm run themes\` writes it from \`themes/default.js\` through
 * \`adaptPalette(palette, 'achromat', ${spec.severityName})\` in \`utils/vision.js\`, and \`npm test\`
 * reports it drifting from what that produces.
 */`;

/** One palette, as the source of a theme module that states every colour it holds. */
export function render(name) {
    const spec = THEMES[name];
    const separations = spec.separation === undefined ? undefined : [spec.separation];
    const palette = adaptPalette(defaultTheme, spec.type, spec.severity, separations);
    const prose = spec.type === 'achromat'
        ? ACHROMAT_PROSE(name, spec)
        : DICHROMAT_PROSE(name, spec,
            lostDistinctions(defaultTheme, defaultTheme, spec.type, spec.severity),
            lostDistinctions(palette, defaultTheme, spec.type, spec.severity));
    const body = Object.entries(palette)
        .map(([key, value]) => `    ${key}: ${JSON.stringify(value)},`)
        .join('\n');
    return `${LICENSE}\n\n${prose}\nexport default {\n${body}\n};\n`;
}

export const NAMES = Object.keys(THEMES);

if (process.argv[1] === fileURLToPath(import.meta.url)) {
    for (const name of NAMES) {
        writeFileSync(join(root, 'themes', `${name}.js`), render(name));
        console.log(`themes/${name}.js`);
    }
}
