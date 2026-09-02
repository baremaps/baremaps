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

import {Color, RGB} from './color.js';

/** A colour stated directly in CIELAB, which is how the reference data below is given. */
class Lab extends Color {
    constructor(l, a, b) {
        super();
        this.lab = {l, a, b};
    }

    toLab() {
        return this.lab;
    }
}

test('a grey keeps its luminance and a colour is replaced by the grey of the same one', () => {
    for (const value of [0, 64, 128, 192, 255]) {
        const grey = Color.fromString(`rgb(${value},${value},${value})`);
        assert.ok(Math.abs(grey.grayscale().toRGB().r - value) < 0.5);
    }
    for (const value of ['rgb(200,30,30)', 'rgb(31,143,40)', 'rgb(0,120,181)']) {
        const color = Color.fromString(value);
        assert.ok(Math.abs(color.grayscale().luminance() - color.luminance()) < 1e-6);
    }
});

test('luminance is the light and not the encoding', () => {
    // Mid grey encodes as 128 and emits about a fifth of the light white does. Weighting the
    // encoded numbers instead, which is what this used to do, answers about a half.
    assert.ok(Math.abs(Color.fromString('rgb(128,128,128)').luminance() - 0.2159) < 0.001);
    assert.equal(Color.fromString('rgb(255,255,255)').luminance().toFixed(4), '1.0000');
    assert.equal(Color.fromString('rgb(0,0,0)').luminance().toFixed(4), '0.0000');
});

test('a colour survives the trip through CIELAB', () => {
    for (const value of ['rgb(200,30,30)', 'rgb(31,143,40)', 'rgb(0,120,181)', 'rgb(242,239,233)']) {
        const color = Color.fromString(value);
        assert.equal(RGB.fromLab(color.toLab(), 1).toString(), value);
    }
});

/**
 * The reference pairs published with the formula.
 *
 *   G. Sharma, W. Wu and E. N. Dalal, "The CIEDE2000 Color-Difference Formula: Implementation
 *   Notes, Supplementary Test Data, and Mathematical Observations", Color Research and Application
 *   30(1), 21-30, 2005, table 1.
 *
 * The three that differ only in the fourth decimal of `a` are the ones that matter: they sit on
 * either side of the turn in the mean hue, which is the branch an implementation gets wrong.
 */
test('CIEDE2000 agrees with the published test data', () => {
    const pairs = [
        [[50, 2.6772, -79.7751], [50, 0, -82.7485], 2.0425],
        [[50, 3.1571, -77.2803], [50, 0, -82.7485], 2.8615],
        [[50, 2.8361, -74.0200], [50, 0, -82.7485], 3.4412],
        [[50, -1.3802, -84.2814], [50, 0, -82.7485], 1.0000],
        [[50, -1.1848, -84.8006], [50, 0, -82.7485], 1.0000],
        [[50, -0.9009, -85.5211], [50, 0, -82.7485], 1.0000],
        [[50, 2.4900, -0.0010], [50, -2.4900, 0.0009], 7.1792],
        [[50, 2.4900, -0.0010], [50, -2.4900, 0.0010], 7.1792],
        [[50, -0.0010, 2.4900], [50, 0.0009, -2.4900], 4.8045],
        [[50, -0.0010, 2.4900], [50, 0.0010, -2.4900], 4.8045],
        [[50, -0.0010, 2.4900], [50, 0.0011, -2.4900], 4.7461],
        [[50, 2.5000, 0.0000], [50, 0.0000, -2.5000], 4.3065],
        [[60.2574, -34.0099, 36.2677], [60.4626, -34.1751, 39.4387], 1.2644],
        [[63.0109, -31.0961, -5.8663], [62.8187, -29.7946, -4.0864], 1.2630],
        [[2.0776, 0.0795, -1.1350], [0.9033, -0.0636, -0.5514], 0.9082],
    ];
    for (const [one, two, expected] of pairs) {
        const difference = new Lab(...one).difference(new Lab(...two));
        assert.ok(Math.abs(difference - expected) < 1e-4,
            `${JSON.stringify(one)} to ${JSON.stringify(two)}: ${difference} is not ${expected}`);
    }
});

/**
 * The bound `vision.js` prunes with: CIEDE2000 is never less than its lightness term, and that term
 * is never divided by more than about 1.75. A pair further apart in lightness than a threshold
 * times that clears the threshold whatever its hues do, which is what makes measuring a whole
 * palette a few hundred times affordable.
 */
test('a difference in lightness is never worth more than 1.75 times the difference it makes', () => {
    let worst = 0;
    for (let l = 0; l <= 100; l += 1) {
        for (const step of [0.5, 1, 2, 4, 8, 16]) {
            if (l + step > 100) continue;
            for (const [a, b] of [[0, 0], [20, 20], [-40, 60], [60, -40], [-80, -80]]) {
                worst = Math.max(worst, step / new Lab(l, a, b).difference(new Lab(l + step, a, b)));
            }
        }
    }
    assert.ok(worst < 1.75, `a lightness difference was worth ${worst} times its CIEDE2000`);
});
