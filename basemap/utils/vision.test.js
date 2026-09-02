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

import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'fs';
import {fileURLToPath} from 'url';

import {Color} from './color.js';
import {simulate, adaptPalette, DICHROMACY, ANOMALY} from './vision.js';

const rgb = (value) => Color.fromString(value).toRGB();
const TYPES = ['protan', 'deutan', 'tritan'];

/**
 * Each row of each published matrix sums to one, which is what makes a grey stay a grey.
 *
 * Read off the source rather than exported, because what is being checked is the transcription:
 * a digit typed wrong anywhere in the three tables moves a row sum by more than this allows.
 */
test('the transcribed matrices are the published ones', () => {
    const source = readFileSync(fileURLToPath(new URL('./vision.js', import.meta.url)), 'utf8');
    const matrices = [...source.matchAll(/\[((?:-?[01]\.\d{6},\s*){8}-?[01]\.\d{6})\]/g)];
    assert.equal(matrices.length, 33, 'three deficiencies at eleven severities each');
    for (const [, body] of matrices) {
        const values = body.split(',').map(Number);
        for (let row = 0; row < 9; row += 3) {
            const sum = values[row] + values[row + 1] + values[row + 2];
            assert.ok(Math.abs(sum - 1) < 1e-5, `row sums to ${sum}`);
        }
    }
});

test('an eye with every cone working changes nothing', () => {
    for (const type of TYPES) {
        assert.equal(simulate(rgb('rgb(200,30,30)'), type, 0).toString(), 'rgb(200,30,30)');
    }
});

test('a grey is a grey to every eye', () => {
    for (const type of TYPES) {
        for (const value of [0, 32, 96, 128, 200, 255]) {
            const grey = simulate(rgb(`rgb(${value},${value},${value})`), type);
            assert.ok(Math.abs(grey.r - value) < 0.01 && Math.abs(grey.b - value) < 0.01,
                `${type} moved rgb(${value},${value},${value}) to ${grey.toString()}`);
        }
    }
});

test('red is a dark yellow to a deuteranope and a protanope', () => {
    // The textbook confusion, and the reason a red-and-green map is a one-colour map to them.
    for (const type of ['deutan', 'protan']) {
        const seen = simulate(rgb('rgb(200,30,30)'), type);
        assert.ok(Math.abs(seen.r - seen.g) < 30, `${type}: ${seen.toString()} is not a yellow`);
        assert.ok(seen.b < seen.r, `${type}: ${seen.toString()} is not dark`);
    }
});

test('the further the cone has shifted, the further the colour moves', () => {
    for (const type of TYPES) {
        const green = Color.fromString('rgb(31,143,40)');
        let previous = 0;
        for (const severity of [0, 0.2, 0.4, 0.6, 0.8, 1]) {
            const moved = green.difference(simulate(green.toRGB(), type, severity));
            assert.ok(moved >= previous - 1e-9, `${type} at ${severity} moved less than before`);
            previous = moved;
        }
    }
});

/**
 * The guarantee the palette is built on: a reader is never handed a worse map than the one they
 * would have had. Separating one pair in lightness can push a colour into a third, so each round
 * is measured and a round that does not improve on the best is thrown away.
 */
test('adapting never loses a reader a distinction it did not save', () => {
    const NOTICEABLE = 2.3;
    const DISTINCT = 5;
    const palette = {
        red: 'rgb(200,30,30)', green: 'rgb(60,140,40)', olive: 'rgb(130,116,20)',
        blue: 'rgb(0,120,181)', purple: 'rgb(126,96,128)', sand: 'rgb(220,206,187)',
        ink: 'rgb(38,38,38)', paper: 'rgb(242,239,233)', teal: 'rgb(63,125,100)',
        rose: 'rgb(205,95,175)', slate: 'rgb(88,119,127)', moss: 'rgb(89,111,82)',
    };
    const keys = Object.keys(palette);
    const original = keys.map((key) => Color.fromString(palette[key]));
    const lost = (drawn, type, severity) => {
        const seen = keys.map((key) => simulate(Color.fromString(drawn[key]).toRGB(), type, severity));
        let count = 0;
        for (let i = 0; i < keys.length; i++) {
            for (let j = i + 1; j < keys.length; j++) {
                if (original[i].difference(original[j]) < DISTINCT) continue;
                if (seen[i].difference(seen[j]) < NOTICEABLE) count++;
            }
        }
        return count;
    };
    for (const type of TYPES) {
        for (const severity of [DICHROMACY, ANOMALY]) {
            const adapted = adaptPalette(palette, type, severity);
            assert.ok(lost(adapted, type, severity) <= lost(palette, type, severity),
                `${type} at ${severity} came out worse than it went in`);
        }
    }
});

test('adapting leaves alone what is not a colour, and keeps the alpha of what is', () => {
    const palette = {
        halo: 'rgba(255, 255, 255, 0.9)',
        stops: [12, 1, 16, 2],
        name: 'not a colour',
        red: 'rgb(200,30,30)',
        green: 'rgb(60,140,40)',
    };
    const adapted = adaptPalette(palette, 'deutan', DICHROMACY);
    assert.deepEqual(adapted.stops, palette.stops);
    assert.equal(adapted.name, palette.name);
    assert.equal(Color.fromString(adapted.halo).toRGB().a, 0.9);
});

test('a monochromat is given the grey of each colour', () => {
    const palette = {red: 'rgb(200,30,30)', green: 'rgb(31,143,40)'};
    const adapted = adaptPalette(palette, 'achromat', DICHROMACY);
    for (const key of Object.keys(palette)) {
        const {r, g, b} = Color.fromString(adapted[key]).toRGB();
        assert.ok(r === g && g === b, `${adapted[key]} is not a grey`);
        // Within what eight bits a channel can hold: the grey is the one of equal luminance,
        // rounded to a colour a display can show.
        assert.ok(Math.abs(Color.fromString(adapted[key]).luminance()
            - Color.fromString(palette[key]).luminance()) < 0.002);
    }
});
