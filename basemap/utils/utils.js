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

export function withSortKeys(directives) {
    return directives
        .map(withFillSortKey)
        .map(withLineSortKey);
}

export function withFillSortKey(directive, index, array) {
    return directive['fill-color'] ?{
        ...directive,
        'fill-sort-key': array.length - index,
    } : directive;
}

export function withLineSortKey(directive, index, array) {
    return directive['line-width'] || directive['line-width-stops'] ? {
        ...directive,
        'line-sort-key': array.length - index,
    } : directive;
}

export function withSymbolSortKeys(directives) {
    return directives.map(withSymbolSortKey);
}

/**
 * The order the directives of a symbol layer win in.
 *
 * Where two symbols want the same piece of the screen MapLibre keeps the one with the lower
 * `symbol-sort-key`, so the key ranks the classes of a layer against each other. A directive says
 * what its class is worth as a `priority`, a small number shared by every class of the same
 * standing, and the position it is written in separates the classes inside that band.
 *
 * Both are needed and neither can do the other's work. The list is ordered by subject and by
 * specificity, because the chain that picks an icon answers with its first hit and a class has to
 * be written above the classes it would otherwise claim; so where a directive is written cannot
 * also say how much of the screen it is worth. Read as a ranking, that order says the first
 * subject written is the most important one, and the icon layer opens on the restaurants.
 *
 * A directive that declares a `symbol-sort-key` of its own keeps it, for a class ranked against
 * the others of its kind by something its features carry rather than by a band: a population, an
 * elevation.
 */
export function withSymbolSortKey(directive, index) {
    // Absent means the directive does not carry the key, the way `mergeDirectives` reads it: a
    // directive asking for the first sort key is asking for something.
    if (directive['symbol-sort-key'] !== undefined) {
        return directive;
    }
    const {priority, ...rest} = directive;
    return {
        ...rest,
        // The band first and the position within it second. The stride is wider than any list of
        // directives this is applied to, so no class reaches the band below by being written late.
        'symbol-sort-key': priority === undefined ? index : priority * 1000 + index,
    };
}

/**
 * Resolve a layer written as one nested object.
 *
 * The layer carries its own `directives`, the list of feature classes it draws and how each is
 * drawn. They are the layer's bulk, but they are still part of it, so they are declared inside it
 * rather than beside it. This turns them into the `filter`, `layout` and `paint` of a style layer
 * and drops the member itself, which the style specification does not define.
 */
export function asLayerObject(layer = {}) {
    const {directives = [], ...baseLayer} = layer;
    return {
        ...baseLayer,
        filter: asFilterProperty(directives, baseLayer['filter']),
        layout: asLayoutProperty(directives, baseLayer['layout']),
        paint: asPaintProperty(directives, baseLayer['paint']),
    };
}

export function asLayoutProperty(directives = [], baseLayout = {}) {
    return Object.assign(
        {
            ...textFont(directives),
            ...textField(directives),
            ...textSize(directives),
            ...textSizeStops(directives),
            ...textMaxWidth(directives),
            ...iconImage(directives),
            ...lineSortKey(directives),
            ...fillSortKey(directives),
            ...symbolSortKey(directives),
        },
        baseLayout,
    )
}

export function asPaintProperty(directives = [], basePaint = {}) {
    return Object.assign(
        {
            ...textColor(directives),
            ...textHaloColor(directives),
            ...textHaloWidth(directives),
            ...iconColor(directives),
            ...fillColor(directives),
            ...fillOutlineColor(directives),
            ...lineColor(directives),
            ...lineWidth(directives),
            ...lineWidthStops(directives),
            ...lineGapWidth(directives),
            ...lineGapWidthStops(directives),
        },
        basePaint,
    )
}

/**
 * The features the layer draws: the ones its own filter admits, that any directive claims.
 *
 * The directives are a disjunction, and an `or` does not care what order it is written in, so every
 * test that reads one attribute is gathered into a single `match` wherever it appears in the list.
 * The icon layer asks two hundred and fifty six questions of every point in the tile; gathered, it
 * asks one per attribute it reads, and each is a lookup rather than a scan.
 */
export function asFilterProperty(directives = [], filter = []) {
    const claims = claimed(directives);
    if (claims && filter.length > 0) {
        return ['all', filter, claims];
    } else if (claims) {
        return claims;
    } else if (filter.length > 0) {
        return filter;
    } else {
        return [];
    }
}

/** The disjunction of a list of tests, with those reading one attribute folded together. */
/**
 * What the directives claim, at the zoom each of them claims it from.
 *
 * A directive that names no `minzoom` is claimed wherever the layer draws, and a list of those is
 * one gathered disjunction, which is what the layer had. A directive that names one is claimed from
 * that zoom up, and the list is then a `step` holding a gathered disjunction per zoom: the classes
 * of a layer need not all arrive at once.
 *
 * The alternative is a zoom test written into each directive's own filter, which would break the
 * gathering: two hundred and fifty six tests, each on one attribute and a zoom, cannot be collected
 * into one lookup per attribute. Grouped this way each zoom still asks one question per attribute
 * it reads.
 */
function claimed(directives) {
    const filters = (kept) => kept.map((directive) => directive['filter']).filter(Boolean);
    const zooms = [...new Set(directives
        .map((directive) => directive['minzoom'])
        .filter((zoom) => zoom !== undefined))].sort((a, b) => a - b);
    if (zooms.length < 2) {
        return union(filters(directives));
    }
    const upTo = (zoom) => union(filters(directives
        .filter((directive) => (directive['minzoom'] ?? zooms[0]) <= zoom)));
    const step = ['step', ['zoom'], upTo(zooms[0])];
    for (const zoom of zooms.slice(1)) {
        step.push(zoom, upTo(zoom));
    }
    return step;
}

function union(filters) {
    const byAttribute = new Map();
    const rest = [];
    for (const filter of filters) {
        const test = asLookup(filter);
        if (!test) {
            rest.push(filter);
            continue;
        }
        if (!byAttribute.has(test.attribute)) byAttribute.set(test.attribute, new Set());
        for (const value of test.values) byAttribute.get(test.attribute).add(value);
    }
    const terms = [...byAttribute].map(([attribute, values]) =>
        ['match', ['get', attribute], asLabel([...values]), true, false]);
    terms.push(...rest);
    if (terms.length === 0) return null;
    return terms.length === 1 ? terms[0] : ['any', ...terms];
}

/**
 * A filter read as one attribute against a set of values, or null if it is not one.
 *
 * This is the shape `match` takes, and nearly every directive in the style has it: an `==`, an `in`
 * over a literal list, or an `any` of those on a single attribute. The `all` wrapper a few of them
 * carry around a single test says nothing and is seen through.
 */
function asLookup(filter) {
    if (!Array.isArray(filter)) return null;
    const [operator, left, right] = filter;
    if (operator === 'all' && filter.length === 2) {
        return asLookup(left);
    }
    if (operator === 'any') {
        const tests = filter.slice(1).map(asLookup);
        if (tests.length > 0 && tests.every((test) => test && test.attribute === tests[0].attribute)) {
            return {attribute: tests[0].attribute, values: tests.flatMap((test) => test.values)};
        }
        return null;
    }
    if (!Array.isArray(left) || left.length !== 2 || left[0] !== 'get' || typeof left[1] !== 'string') {
        return null;
    }
    if (operator === '==' && isLabel(right)) {
        return {attribute: left[1], values: [right]};
    }
    if (operator === 'in' && Array.isArray(right) && right[0] === 'literal' &&
        Array.isArray(right[1]) && right[1].length > 0 && right[1].every(isLabel)) {
        return {attribute: left[1], values: right[1]};
    }
    return null;
}

/** A `match` label is a string or a number, and `match` will not mix the two. */
function isLabel(value) {
    return typeof value === 'string' || typeof value === 'number';
}

/** One value is written as itself, several as the list `match` reads as one branch. */
function asLabel(values) {
    return values.length === 1 ? values[0] : values;
}

/**
 * A list of tests and the value each yields, as one expression.
 *
 * Written as a `case` this is a linear scan: a feature is compared against every test above the one
 * that answers for it. Consecutive tests reading the same attribute are folded into a `match`,
 * which MapLibre resolves by lookup. Only consecutive ones: `case` returns its first match, so
 * gathering tests that are not already adjacent would move a rule past another one that a feature
 * carrying both tags matches as well, and change what that feature is drawn as.
 */
function asChain(pairs, fallback) {
    const segments = [];
    for (const [filter, value] of pairs) {
        const test = asLookup(filter);
        const last = segments[segments.length - 1];
        if (test && last && last.attribute === test.attribute) {
            last.branches.push([test.values, value]);
        } else if (test) {
            segments.push({attribute: test.attribute, branches: [[test.values, value]]});
        } else if (last && !last.attribute) {
            last.branches.push([filter, value]);
        } else {
            segments.push({attribute: null, branches: [[filter, value]]});
        }
    }
    let expression = fallback;
    for (const segment of segments.reverse()) {
        expression = segment.attribute === null
            ? ['case', ...segment.branches.flat(), expression]
            : ['match', ['get', segment.attribute], ...asBranches(segment.branches), expression];
    }
    return expression;
}

/**
 * The branches of a `match`, with each value kept by the first branch that claims it.
 *
 * A `case` gives a repeated test to whichever rule was written first and a `match` refuses to be
 * given the same label twice, so a value claimed again lower down is dropped here rather than
 * rejected by the specification.
 */
function asBranches(branches) {
    const claimed = new Set();
    const flat = [];
    for (const [values, value] of branches) {
        const fresh = values.filter((label) => !claimed.has(label));
        if (fresh.length === 0) continue;
        for (const label of fresh) claimed.add(label);
        flat.push(asLabel(fresh), value);
    }
    return flat;
}

/**
 * The fallback is the empty name rather than a word, which MapLibre reads as no icon. `'none'` is
 * an image name like any other, so a feature reaching the fallback asked for an icon called none,
 * which the sprite does not hold and the console reports once for every tile that carries one.
 */
function iconImage(directives) {
    return mergeDirectives(directives, 'icon-image', '')
}

function iconColor(directives) {
    return mergeDirectives(directives, 'icon-color', 'rgba(0, 0, 0, 0)')
}

/**
 * The fallback is a stack and not a name: `text-font` is an array, and a bare string there is a
 * style the specification rejects. Nothing reaches this today, every layer naming its font in
 * `layout`, so the wrong shape sat here unbuilt.
 */
/**
 * The font a directive asks its class to be set in.
 *
 * A font is named by a stack, which is a list, and a list at the head of an expression is read as a
 * call: gathered into a chain unwrapped, `['Noto Sans Bold']` is an expression by that name and the
 * style is rejected. So the stacks are wrapped, both the ones the directives name and the one they
 * fall through to. Every other property gathered this way holds a number or a colour, which cannot
 * be mistaken for a call and needs no wrapping.
 */
function textFont(directives) {
    const stack = (font) =>
        Array.isArray(font) && font[0] !== 'literal' ? ['literal', font] : font;
    const named = directives.map((directive) => directive['text-font'] === undefined
        ? directive
        : {...directive, 'text-font': stack(directive['text-font'])});
    return mergeDirectives(named, 'text-font', stack(['Noto Sans Regular']))
}

function textField(directives) {
    return mergeDirectives(directives, 'text-field', null)
}

function textMaxWidth(directives) {
    return mergeDirectives(directives, 'text-max-width', 4)
}

function textColor(directives) {
    return mergeDirectives(directives, 'text-color', 'rgba(0, 0, 0, 0)')
}

function textHaloColor(directives) {
    return mergeDirectives(directives, 'text-halo-color', 'rgba(0, 0, 0, 0)')
}

function textHaloWidth(directives) {
    return mergeDirectives(directives, 'text-halo-width', 0)
}

function fillColor(directives) {
    return mergeDirectives(directives, 'fill-color', 'rgba(0, 0, 0, 0)')
}

function fillOutlineColor(directives) {
    return mergeDirectives(directives, 'fill-outline-color', 'rgba(0, 0, 0, 0)')
}

function lineColor(directives) {
    return mergeDirectives(directives, 'line-color', 'rgba(0, 0, 0, 0)')
}

function lineWidth(directives) {
    return mergeDirectives(directives, 'line-width', 0)
}

function lineGapWidth(directives) {
    return mergeDirectives(directives, 'line-gap-width', 0)
}

function lineSortKey(directives) {
    return mergeDirectives(directives, 'line-sort-key', 0)
}

function fillSortKey(directives) {
    return mergeDirectives(directives, 'fill-sort-key', 0)
}

function symbolSortKey(directives) {
    return mergeDirectives(directives, 'symbol-sort-key', 0)
}

function textSize(directives) {
    return mergeDirectives(directives, 'text-size', 0)
}

/**
 * One style property, gathered from every directive that sets it.
 *
 * A directive that does not set the property is absent from the chain and falls through to the
 * value given here. Absent means the directive does not carry the key at all: a directive asking
 * for a width of zero or the first sort key is asking for something, and reading it as absence
 * dropped it silently.
 */
function mergeDirectives(directives, property, value) {
    const pairs = directives.flatMap((rule) =>
        rule[property] === undefined ? [] : [[rule['filter'], rule[property]]]);
    if (pairs.length === 0) {
        return {};
    }
    return {
        [property]: asChain(pairs, value),
    }
}

function lineWidthStops(directives) {
    return interpolateStops(directives, 'line-width-stops', 'line-width', 1)
}

function lineGapWidthStops(directives) {
    return interpolateStops(directives, 'line-gap-width-stops', 'line-gap-width', 1)
}

function textSizeStops(directives) {
    return interpolateStops(directives, 'text-size-stops', 'text-size', 1)
}

/**
 * Determine the zoom levels at which the merged curve changes slope.
 *
 * Every stop carries a copy of the whole filter set, so emitting one per zoom
 * level makes the filters, rather than the values, the bulk of the style. The
 * curve produced by `interpolate` is piecewise linear, so only the zoom levels
 * where it bends need a stop; the rest are reproduced exactly by the linear
 * interpolation between them.
 *
 * A curve bends at each of its breakpoints, at the zoom just below the first
 * breakpoint (below which the value is flat at zero), and at every integer zoom
 * past the last breakpoint, where the value doubles rather than growing linearly.
 */
function interpolationZooms(activeDirectives, property) {
    const zooms = new Set([0, 22]);
    for (const directive of activeDirectives) {
        const stops = directive[property];
        if (stops[0] - 1 >= 0) {
            zooms.add(stops[0] - 1);
        }
        for (let i = 0; i < stops.length; i += 2) {
            zooms.add(stops[i]);
        }
        for (let zoom = stops[stops.length - 2] + 1; zoom <= 22; zoom++) {
            zooms.add(zoom);
        }
    }
    return [...zooms].filter((zoom) => zoom >= 0 && zoom <= 22).sort((a, b) => a - b);
}

function interpolateStops(directives, property, alias, value) {
    const activeDirectives = directives.filter((directive) => directive[property]);
    if (activeDirectives.length == 0) {
        return {};
    }
    var mergedDirective = [
        'interpolate',
        ['linear'],
        ['zoom'],
    ];
    for (let zoom of interpolationZooms(activeDirectives, property)) {
        mergedDirective.push(zoom);
        mergedDirective.push(asChain(
            activeDirectives.map((directive) => [directive['filter'], interpolate(zoom, directive[property])]),
            value,
        ));
    }
    return {
        [alias]: mergedDirective,
    }
}

/**
 * Given an array in the form of [zoom_level_n, value_n, zoom_level_m, value_m, ...], with n < m, n >= 0, and m <= 22,
 * the function linearly interpolates the value for the given zoom level.
 *
 * The values before zoom_level_n are assumed to be equal to 0.
 * The values after zoom_level_m are assumed to be equal to value_m * 2 ** (zoom - zoom_level_m).
 *
 * Here are a few examples:
 * interpolate(0, [10, 1, 14, 5]) = 0
 * interpolate(9, [10, 1, 14, 5]) = 0
 * interpolate(10, [10, 1, 14, 5]) = 1
 * interpolate(11, [10, 1, 14, 5]) = 2
 * interpolate(12, [10, 1, 14, 5]) = 3
 * interpolate(13, [10, 1, 14, 5]) = 4
 * interpolate(14, [10, 1, 14, 5]) = 5
 * interpolate(15, [10, 1, 14, 5]) = 10
 * interpolate(17, [10, 1, 14, 5]) = 40
 * interpolate(22, [10, 1, 14, 5]) = 5
 */
export function interpolate(zoom, values) {
    let i = 0
    while (i < values.length && zoom >= values[i]) {
        i += 2;
    }
    if (i >= values.length) {
        return values[values.length - 1] * 2 ** (zoom - values[values.length - 2]);
    }
    if (i === 0) {
        return 0;
    }
    const zoomN = values[i - 2];
    const valueN = values[i - 1];
    const zoomM = values[i];
    const valueM = values[i + 1];
    return valueN + (valueM - valueN) * (zoom - zoomN) / (zoomM - zoomN);
}

