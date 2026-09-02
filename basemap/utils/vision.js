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
 * Two models of colour vision: what a colour looks like to an eye with fewer working cone types,
 * and what to draw instead so that such an eye keeps the distinction the colour carries.
 *
 * The two answer different questions and a theme wants the second. A map put through the first is
 * a preview for the designer, and handing it to the reader it depicts applies the deficiency
 * twice, once by the palette and once by the eye. The themes here name a reader, not a simulation.
 *
 * `simulate` is the instrument. `adapt` is the map.
 */

import {Color, RGB, differenceLab} from './color.js';

// The sRGB transfer function, IEC 61966-2-1. Every model below is defined on the light a display
// emits and not on the numbers encoding it. A matrix applied to the encoded numbers is a different
// transform that happens to look plausible, which is what the eight hand-written matrices this
// file replaces were: their coefficients came from a web filter of unclear provenance, they were
// applied to gamma-encoded values, and none of them holds a grey.
const toLinear = (value) => {
    const v = value / 255;
    return v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4;
};

const fromLinear = (value) => {
    const v = Math.min(Math.max(value, 0), 1);
    return 255 * (v <= 0.0031308 ? v * 12.92 : 1.055 * v ** (1 / 2.4) - 0.055);
};

/**
 * How far a cone has shifted, from a full set of working cones to none of that kind.
 *
 * Machado's model is one model across the range rather than two. A colour vision deficiency is a
 * cone whose response curve has moved towards its neighbour's, and the severity is how far; at 1
 * the two curves coincide and the eye is a dichromat's. Most people who have one of these are not
 * at 1, which is why the anomalous themes are drawn for the middle of the range and not the end.
 */
export const DICHROMACY = 1;
export const ANOMALY = 0.6;

/**
 * Machado's transformation matrices, one row per tenth of severity, applied to linear RGB.
 *
 *   G. M. Machado, M. M. Oliveira and L. A. F. Fernandes, "A Physiologically-based Model for
 *   Simulation of Color Vision Deficiency", IEEE Transactions on Visualization and Computer
 *   Graphics 15(6), 1291-1298, 2009. doi:10.1109/TVCG.2009.113
 *
 * These are the precomputed matrices the authors publish with the paper, at
 * https://www.inf.ufrgs.br/~oliveira/pubs_files/CVD_Simulation/CVD_Simulation.html, as carried by
 * the `colorspace` R package and by `culori`.
 *
 * A table and not a formula, because the model is not linear in severity: the tritan rows turn
 * back on themselves between a third and a half, so a matrix read off the two ends would be wrong
 * across the middle.
 */
const PROTAN = [
    [1.000000, 0.000000, 0.000000, 0.000000, 1.000000, 0.000000, 0.000000, 0.000000, 1.000000],
    [0.856167, 0.182038, -0.038205, 0.029342, 0.955115, 0.015544, -0.002880, -0.001563, 1.004443],
    [0.734766, 0.334872, -0.069637, 0.051840, 0.919198, 0.028963, -0.004928, -0.004209, 1.009137],
    [0.630323, 0.465641, -0.095964, 0.069181, 0.890046, 0.040773, -0.006308, -0.007724, 1.014032],
    [0.539009, 0.579343, -0.118352, 0.082546, 0.866121, 0.051332, -0.007136, -0.011959, 1.019095],
    [0.458064, 0.679578, -0.137642, 0.092785, 0.846313, 0.060902, -0.007494, -0.016807, 1.024301],
    [0.385450, 0.769005, -0.154455, 0.100526, 0.829802, 0.069673, -0.007442, -0.022190, 1.029632],
    [0.319627, 0.849633, -0.169261, 0.106241, 0.815969, 0.077790, -0.007025, -0.028051, 1.035076],
    [0.259411, 0.923008, -0.182420, 0.110296, 0.804340, 0.085364, -0.006276, -0.034346, 1.040622],
    [0.203876, 0.990338, -0.194214, 0.112975, 0.794542, 0.092483, -0.005222, -0.041043, 1.046265],
    [0.152286, 1.052583, -0.204868, 0.114503, 0.786281, 0.099216, -0.003882, -0.048116, 1.051998],
];

const DEUTAN = [
    [1.000000, 0.000000, 0.000000, 0.000000, 1.000000, 0.000000, 0.000000, 0.000000, 1.000000],
    [0.866435, 0.177704, -0.044139, 0.049567, 0.939063, 0.011370, -0.003453, 0.007233, 0.996220],
    [0.760729, 0.319078, -0.079807, 0.090568, 0.889315, 0.020117, -0.006027, 0.013325, 0.992702],
    [0.675425, 0.433850, -0.109275, 0.125303, 0.847755, 0.026942, -0.007950, 0.018572, 0.989378],
    [0.605511, 0.528560, -0.134071, 0.155318, 0.812366, 0.032316, -0.009376, 0.023176, 0.986200],
    [0.547494, 0.607765, -0.155259, 0.181692, 0.781742, 0.036566, -0.010410, 0.027275, 0.983136],
    [0.498864, 0.674741, -0.173604, 0.205199, 0.754872, 0.039929, -0.011131, 0.030969, 0.980162],
    [0.457771, 0.731899, -0.189670, 0.226409, 0.731012, 0.042579, -0.011595, 0.034333, 0.977261],
    [0.422823, 0.781057, -0.203881, 0.245752, 0.709602, 0.044646, -0.011843, 0.037423, 0.974421],
    [0.392952, 0.823610, -0.216562, 0.263559, 0.690210, 0.046232, -0.011910, 0.040281, 0.971630],
    [0.367322, 0.860646, -0.227968, 0.280085, 0.672501, 0.047413, -0.011820, 0.042940, 0.968881],
];

const TRITAN = [
    [1.000000, 0.000000, 0.000000, 0.000000, 1.000000, 0.000000, 0.000000, 0.000000, 1.000000],
    [0.926670, 0.092514, -0.019184, 0.021191, 0.964503, 0.014306, 0.008437, 0.054813, 0.936750],
    [0.895720, 0.133330, -0.029050, 0.029997, 0.945400, 0.024603, 0.013027, 0.104707, 0.882266],
    [0.905871, 0.127791, -0.033662, 0.026856, 0.941251, 0.031893, 0.013410, 0.148296, 0.838294],
    [0.948035, 0.089490, -0.037526, 0.014364, 0.946792, 0.038844, 0.010853, 0.193991, 0.795156],
    [1.017277, 0.027029, -0.044306, -0.006113, 0.958479, 0.047634, 0.006379, 0.248708, 0.744913],
    [1.104996, -0.046633, -0.058363, -0.032137, 0.971635, 0.060503, 0.001336, 0.317922, 0.680742],
    [1.193214, -0.109812, -0.083402, -0.058496, 0.979410, 0.079086, -0.002346, 0.403492, 0.598854],
    [1.257728, -0.139648, -0.118081, -0.078003, 0.975409, 0.102594, -0.003316, 0.501214, 0.502102],
    [1.278864, -0.125333, -0.153531, -0.084748, 0.957674, 0.127074, -0.000989, 0.601151, 0.399838],
    [1.255528, -0.076749, -0.178779, -0.078411, 0.930809, 0.147602, 0.004733, 0.691367, 0.303900],
];

const TABLES = {protan: PROTAN, deutan: DEUTAN, tritan: TRITAN};

/** The matrix for a severity between two rows of the table, read along the line joining them. */
function matrix(type, severity) {
    const table = TABLES[type];
    if (!table) {
        throw new Error(`Unknown deficiency: ${type}`);
    }
    const position = Math.min(Math.max(severity, 0), 1) * 10;
    const low = Math.floor(position);
    const high = Math.min(low + 1, table.length - 1);
    const t = position - low;
    return table[low].map((value, i) => value + (table[high][i] - value) * t);
}

const apply = (m, [r, g, b]) => [
    m[0] * r + m[1] * g + m[2] * b,
    m[3] * r + m[4] * g + m[5] * b,
    m[6] * r + m[7] * g + m[8] * b,
];

const linear = (rgb) => [toLinear(rgb.r), toLinear(rgb.g), toLinear(rgb.b)];
const encoded = (channels, alpha) => new RGB(...channels.map(fromLinear), alpha);
const mix = (from, to, t) => from.map((value, i) => value + (to[i] - value) * t);

/** Rec.709 luminance: the sRGB primaries weighted by how much light the eye takes from each. */
export const LUMINANCE = [0.2126, 0.7152, 0.0722];

/**
 * What a colour looks like to the eye named by `type` at this `severity`.
 *
 * `achromat` is the eye that reads luminance and nothing else, so a colour becomes the grey of
 * equal luminance. The other three are Machado's model.
 */
export function simulate(rgb, type, severity = DICHROMACY) {
    const source = linear(rgb);
    if (type === 'achromat') {
        const grey = LUMINANCE[0] * source[0] + LUMINANCE[1] * source[1] + LUMINANCE[2] * source[2];
        return encoded(mix(source, [grey, grey, grey], Math.min(Math.max(severity, 0), 1)), rgb.a);
    }
    return encoded(apply(matrix(type, severity), source), rgb.a);
}

/**
 * A difference a reader notices at all, and one two classes of a map are meant to have.
 *
 * The first is the usual reading of CIEDE2000: below about 2.3 two colours are the same colour.
 * The second is the distance at which two colours were saying different things in the palette they
 * came from, so that a pair that was never distinct is not counted as one the eye has lost.
 */
const NOTICEABLE = 2.3;
const DISTINCT = 5;

// How far apart in CIELAB lightness two colours are pushed when the eye reading them has no other
// way to tell them apart. Around two and a half is the smallest difference in lightness alone that
// is noticed at all; above that, a wider push separates the pair in hand and is more likely to
// drive one of them into a third colour, and which distance comes out ahead depends on how crowded
// the palette is under the condition being drawn for. So the palette is built at each of them and
// the one that loses the reader the fewest distinctions is kept.
const SEPARATIONS = [2.5, 4, 6, 9];

// The largest the lightness term of CIEDE2000 is ever divided by, which it reaches at either end
// of the scale. A difference in L* larger than a threshold times this clears the threshold whatever
// the two colours do in hue, which is what lets most of a palette be dismissed without working the
// formula out.
const SL_MAX = 1.75;

/**
 * The distinctions a reader loses from a palette.
 *
 * A pair of colours the reference palette draws far enough apart to be saying different things,
 * which arrives at the same colour once this eye is simulated over the palette being measured. The
 * reference is what the map means to say; the palette is what it says. Passing the same one twice
 * measures a palette against itself, which is what a reader gets when nothing is done for them.
 *
 * This is what `adaptPalette` is built to reduce, what `validate.js` reports on, and what the
 * generated palettes record in their own headers, so all three count the same thing.
 */
export function lostDistinctions(palette, reference, type, severity = DICHROMACY) {
    const keys = Object.keys(reference).filter((key) => Color.fromString(reference[key]) !== null);
    const meant = keys.map((key) => Color.fromString(reference[key]).toLab());
    const seen = keys.map((key) =>
        simulate(Color.fromString(palette[key]).toRGB(), type, severity).toLab());
    let lost = 0;
    for (let i = 0; i < keys.length; i++) {
        for (let j = i + 1; j < keys.length; j++) {
            if (differenceLab(meant[i], meant[j]) < DISTINCT) continue;
            if (differenceLab(seen[i], seen[j]) < NOTICEABLE) lost++;
        }
    }
    return lost;
}

/**
 * The palette to draw for a reader with this deficiency.
 *
 * The obvious transform is daltonisation: simulate the eye, take the difference between what the
 * colour is and what the eye makes of it, and add that difference back along an axis the eye can
 * still read. It is what the GIMP and Krita filters do, after Fidaner, Lin and Ozguven, "Analysis
 * of Color Blindness" (2005), and on this palette it makes things worse. Measured as the number of
 * pairs a reader could tell apart in the default palette and cannot tell apart after simulating
 * their vision, it took protanopia from 162 to 202 and deuteranopia from 135 to 186, at every
 * strength between a fifth and the whole of it. Daltonisation is built to exaggerate differences
 * inside a photograph; a palette already spans the gamut, so the correction mostly drives colours
 * into each other and into the walls.
 *
 * What every one of these conditions leaves untouched is lightness. So the palette is not moved as
 * a whole: it is measured, and only the colours that actually collide are moved, along the one axis
 * the reader still has. Two colours conflict when they were far enough apart to be saying different
 * things and land close enough together, once the eye is simulated, to be saying the same one.
 * Conflicts chain -- a is lost against b and b against c -- so they are gathered into groups, and a
 * group is spread across lightness around where it already sat, which keeps the map as light as it
 * was while giving the reader back the distinction.
 *
 * A monochromat is left with luminance and nothing else, so their palette is the grey of each
 * colour and the spreading has nothing to work with: two classes of identical lightness cannot be
 * separated by lightness. What that reader needs is a palette whose classes are far enough apart in
 * lightness to begin with, which is a thing to draw and not a thing to compute; `validate.js`
 * reports the pairs that are not.
 */
export function adaptPalette(palette, type, severity = DICHROMACY, separations = SEPARATIONS) {
    const entries = Object.entries(palette);
    const parsed = entries.map(([, value]) => Color.fromString(value));

    if (type === 'achromat') {
        return Object.fromEntries(entries.map(([key, value], i) =>
            [key, parsed[i] === null ? value : simulate(parsed[i].toRGB(), type, severity).toString()]));
    }

    const indices = parsed.map((color, i) => color === null ? -1 : i).filter((i) => i >= 0);
    const lab = new Map(indices.map((i) => [i, parsed[i].toLab()]));
    const source = new Map(indices.map((i) => [i, lab.get(i).l]));
    const at = (i, l) => Math.abs(l - source.get(i)) < 0.01 ? parsed[i] : parsed[i].withLightness(l);

    /**
     * Every pair of colours whose lightnesses are within `window` of each other, as index pairs.
     *
     * CIEDE2000 is never less than its lightness term, and that term is never divided by more than
     * `SL_MAX`, so two colours further apart in L* than a threshold times that clear the threshold
     * on lightness alone. Sorting by lightness and walking a window over it therefore reaches every
     * pair that could be under the threshold without looking at the rest, which for a palette of
     * this size is a few hundred pairs where all of them is thirty-seven thousand. The whole
     * palette is measured a few dozen times while it settles, and this is what makes that
     * affordable in an interpreter.
     */
    const near = (lightnessOf, window) => {
        const order = [...indices].sort((i, j) => lightnessOf(i) - lightnessOf(j));
        const pairs = [];
        for (let a = 0; a < order.length; a++) {
            for (let b = a + 1; b < order.length; b++) {
                if (lightnessOf(order[b]) - lightnessOf(order[a]) > window) break;
                // Ordered by index and not by lightness: a pair is named the same whichever of the
                // two orderings reaches it, so that the pairs found under one lightness can be
                // looked up among the pairs found under another.
                pairs.push(order[a] < order[b] ? [order[a], order[b]] : [order[b], order[a]]);
            }
        }
        return pairs;
    };

    // The pairs the palette was never drawing apart. A distinction that was not there cannot be
    // lost, so these are the pairs a collision does not count against, and they are the small set:
    // stated this way round rather than as the pairs that are distinct, which is almost all of them.
    const same = new Set();
    for (const [i, j] of near((i) => source.get(i), DISTINCT * SL_MAX)) {
        if (differenceLab(lab.get(i), lab.get(j)) < DISTINCT) {
            same.add(`${i},${j}`);
        }
    }

    /** The distinctions this reader loses at a given set of lightnesses. */
    const conflictsAt = (assignment) => {
        const seen = new Map(indices.map((i) =>
            [i, simulate(at(i, assignment.get(i)).toRGB(), type, severity).toLab()]));
        const found = [];
        for (const [i, j] of near((i) => seen.get(i).l, NOTICEABLE * SL_MAX)) {
            if (same.has(`${i},${j}`)) continue;
            if (differenceLab(seen.get(i), seen.get(j)) >= NOTICEABLE) continue;
            found.push([i, j]);
        }
        return found;
    };

    // Each conflicting pair is pushed apart in lightness until it is separated, both colours moving
    // by half of what is missing so that neither carries the whole change. A colour in several
    // conflicts is pulled by each of them and settles between them.
    const relax = (assignment, conflicts, separation) => {
        const moved = new Map(assignment);
        for (let pass = 0; pass < 40; pass++) {
            let changed = false;
            for (const [i, j] of conflicts) {
                const [low, high] = moved.get(i) <= moved.get(j) ? [i, j] : [j, i];
                const gap = moved.get(high) - moved.get(low);
                if (gap >= separation) continue;
                const push = (separation - gap) / 2;
                moved.set(low, Math.max(moved.get(low) - push, 0));
                moved.set(high, Math.min(moved.get(high) + push, 100));
                changed = true;
            }
            if (!changed) break;
        }
        return moved;
    };

    // Separating one pair can push a colour into a third it did not collide with, so the palette is
    // measured again after each round and only kept while it is improving. The palette a reader is
    // given is never worse than the one they would have had: a round that fails to improve on the
    // best so far is discarded, and the best is what is returned.
    const initial = conflictsAt(source).length;
    let best = source;
    let fewest = initial;
    for (const separation of separations) {
        let current = source;
        // Against what this separation started from, and not against what another one reached:
        // measured against the best so far, a separation that improves on the palette but not on
        // its predecessor is thrown away in its first round and never gets to its second.
        let remaining = initial;
        for (let round = 0; round < 6 && remaining > 0; round++) {
            const candidate = relax(current, conflictsAt(current), separation);
            const after = conflictsAt(candidate).length;
            if (after >= remaining) break;
            current = candidate;
            remaining = after;
        }
        if (remaining < fewest) {
            best = current;
            fewest = remaining;
        }
    }

    return Object.fromEntries(entries.map(([key, value], i) => {
        if (!best.has(i)) return [key, value];
        const color = at(i, best.get(i));
        return [key, color === parsed[i] ? value : color.toString()];
    }));
}
