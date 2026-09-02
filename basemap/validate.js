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
 * Build-time checks for the style and the theme.
 *
 * Run with `node validate.js`. Exits non-zero if any error is reported.
 *
 * The style is assembled from plain JavaScript, so nothing catches a mistyped
 * theme key or a filter that can never match: `mergeDirectives` silently drops
 * a property whose value is `undefined`, and MapLibre then falls back to its
 * own default (black, for a line or fill colour). These checks exist to turn
 * that class of silent visual corruption into a build failure.
 *
 * They also replace deduplication as the defence against drift. The highway
 * layers deliberately repeat their directive lists so that a single road class
 * can be adjusted without disturbing the others; `parityGroups` below reports
 * when those lists diverge instead of preventing the divergence.
 */
import {existsSync, readFileSync, readdirSync, statSync} from 'fs';
import {join, dirname} from 'path';
import {fileURLToPath} from 'url';

import theme, {fixed} from './theme.js';
import map from './map.js';
import {Color} from './utils/color.js';

// The style the map describes, derived the way MapCompiler derives it: the map-wide properties a
// renderer uses, and the layers with the source filled in and the tileset extensions dropped. This
// mirrors what MapCompiler does, so that the spec check below validates what MapLibre is served.
const source = map.source ?? {};
const id = source.id ?? 'baremaps';
const sources = {[id]: {type: 'vector', url: source.url}};
// The terrain is a second source, traced from elevation rather than queried, so a layer naming it
// has to find it here or the specification check below reports a source that does not exist.
if (map.terrain) {
    sources[map.terrain.id ?? 'terrain'] = {
        type: 'vector',
        tiles: map.terrain.tiles,
        minzoom: map.terrain.minzoom,
        maxzoom: map.terrain.maxzoom,
        attribution: map.terrain.attribution,
    };
}
/**
 * The zoom each source layer's features begin at, read off the queries it is declared with.
 * MapCompiler gives a layer that names no minzoom the one belonging to what it reads, so the same
 * is done here for the checks below to see the style MapLibre is served.
 */
const floors = {};
const terrainId = map.terrain ? (map.terrain.id ?? 'terrain') : null;
for (const layer of map.layers) {
    if (!layer.sourceLayer) continue;
    let floor;
    if (terrainId && layer.source === terrainId) {
        floor = map.terrain.minzoom ?? 0;
    } else if (layer.sourceQueries) {
        floor = Math.min(...layer.sourceQueries.map((query) => query.minzoom ?? 0));
    } else {
        continue;
    }
    floors[layer.sourceLayer] = Math.min(floors[layer.sourceLayer] ?? floor, floor);
}

const style = {
    version: 8,
    name: map.name,
    center: map.center,
    zoom: map.zoom,
    sprite: map.sprite,
    glyphs: map.glyphs,
    sources,
    layers: map.layers.map(({sourceQueries, sourceSchema, sourceLayer, ...layer}) =>
        ({
            ...layer,
            'source-layer': sourceLayer,
            source: layer.source ?? id,
            minzoom: layer.minzoom ?? (floors[sourceLayer] || undefined),
        })),
};

const root = dirname(fileURLToPath(import.meta.url));

const errors = [];
const warnings = [];
const error = (check, message) => errors.push({check, message});
const warn = (check, message) => warnings.push({check, message});

/**
 * Layer groups that describe the same features through different presentations
 * (a road, its tunnel, its bridge). They are kept as separate files on purpose,
 * so this reports a class covered by some members and not others rather than
 * forcing them to share a definition. Record a deliberate omission in `accept`
 * to silence it, which keeps the decision visible in the diff.
 */
const parityGroups = [
    {
        name: 'highway line',
        attribute: 'highway',
        layers: ['highway_line', 'highway_tunnel_line', 'highway_bridge_line'],
        accept: [],
    },
    {
        name: 'highway outline',
        attribute: 'highway',
        layers: ['highway_outline', 'highway_tunnel_outline', 'highway_bridge_outline'],
        accept: [],
    },
];

// --- helpers ---------------------------------------------------------------

/** Every theme file, paired with its source, which says which colours it names itself. */
function themeFiles() {
    return readdirSync(join(root, 'themes')).filter((file) => file.endsWith('.js')).sort()
        .map((file) => [file, readFileSync(join(root, 'themes', file), 'utf8')]);
}

function layerFiles() {
    const files = [];
    for (const dir of readdirSync(join(root, 'layers'))) {
        const path = join(root, 'layers', dir);
        if (!statSync(path).isDirectory()) continue;
        for (const file of readdirSync(path)) {
            if (file.endsWith('.js')) {
                files.push(`layers/${dir}/${file}`);
            }
        }
    }
    return files.sort();
}

/** Walk every nested array of a compiled expression tree. */
function walk(node, visit) {
    if (!Array.isArray(node)) return;
    visit(node);
    for (const child of node) walk(child, visit);
}

function expressionsOf(layer) {
    return [layer.filter, ...Object.values(layer.paint ?? {}), ...Object.values(layer.layout ?? {})];
}

// --- checks ----------------------------------------------------------------

/**
 * Every `theme.x` read by a layer must exist. A miss yields `undefined`, which
 * `mergeDirectives` drops, which MapLibre renders as its default colour.
 */
function checkThemeReferences(files) {
    const defined = new Set(Object.keys(theme));
    const used = new Map();

    for (const file of files) {
        const source = readFileSync(join(root, file), 'utf8')
            // Drop imports so that `./theme.js` is not read as a `theme.js` key.
            .replace(/^\s*import[^\n]*$/gm, '');
        for (const match of source.matchAll(/\btheme\.([A-Za-z_][A-Za-z0-9_]*)/g)) {
            if (!used.has(match[1])) used.set(match[1], new Set());
            used.get(match[1]).add(file);
        }
    }

    for (const [key, where] of [...used].sort()) {
        if (!defined.has(key)) {
            error('theme-reference', `theme.${key} is not defined  (${[...where].join(', ')})`);
        }
    }

    const unused = [...defined].filter((key) => !used.has(key)).sort();
    if (unused.length) {
        warn('theme-unused', `${unused.length} theme keys are never referenced: ${unused.join(', ')}`);
    }
}

/**
 * Theme values must be the shape the style expects: a colour string that parses,
 * or a `[zoom, value, ...]` stop array with ascending zooms.
 *
 * Every theme is checked, not just the active one. A derived theme copies any
 * value it cannot parse, so a colour that fails to round-trip does not raise an
 * error there; it quietly leaves that entry at its parent's value, and any theme
 * derived from it in turn inherits the same gap.
 */
async function checkThemeValues() {
    const names = readdirSync(join(root, 'themes')).filter((file) => file.endsWith('.js')).sort();

    for (const name of names) {
        const values = (await import(`./themes/${name}`)).default;
        const label = `themes/${name}`;

        for (const [key, value] of Object.entries(values)) {
            if (Array.isArray(value)) {
                if (value.length < 2 || value.length % 2 !== 0) {
                    error('theme-value', `${label}: ${key} is not a [zoom, value, ...] pair list`);
                    continue;
                }
                for (let i = 0; i < value.length; i += 2) {
                    if (typeof value[i] !== 'number' || typeof value[i + 1] !== 'number') {
                        error('theme-value', `${label}: ${key} has a non-numeric entry at index ${i}`);
                    } else if (i >= 2 && value[i] <= value[i - 2]) {
                        error('theme-value',
                            `${label}: ${key} zoom stops are not ascending (${value[i - 2]} then ${value[i]})`);
                    }
                }
            } else if (typeof value === 'string') {
                const colour = Color.fromString(value);
                if (colour === null) {
                    error('theme-value', `${label}: ${key} is not a parsable colour: ${JSON.stringify(value)}`);
                } else if (Color.fromString(colour.toRGB().toString()) === null) {
                    error('theme-value',
                        `${label}: ${key} does not round-trip, so themes derived from this one ` +
                        `will silently keep the parent value: ${JSON.stringify(value)}`);
                }
            } else {
                error('theme-value', `${label}: ${key} is neither a colour nor a stop array`);
            }
        }

        const missing = Object.keys(theme).filter((key) => !(key in values));
        if (missing.length) {
            error('theme-value', `${label} is missing ${missing.length} keys held by the active theme: ` +
                missing.slice(0, 5).join(', ') + (missing.length > 5 ? ', ...' : ''));
        }
    }
}

/** The theme keys a theme file writes out itself, as opposed to the ones it derives or inherits. */
function statedKeys(source) {
    return new Set([...source.matchAll(/^\s+([A-Za-z][A-Za-z0-9_]*):/gm)].map((match) => match[1]));
}

/**
 * A theme that names any colour has to name them all. Such a theme is a palette rather than a
 * transform: it does not follow the default theme, it answers it colour by colour, and a colour it
 * leaves out does not fall back to something neutral, it arrives from the default theme with the
 * hue that theme chose for a reason this one does not share. One key inherited into a palette is a
 * single feature drawn in another map's colours, which is the kind of thing that goes unseen until
 * someone finds it on a tile.
 *
 * The hillshade is not counted, `theme.js` holding those colours out of every theme.
 */
function checkThemePalettes(sources) {
    const colours = Object.keys(theme).filter((key) => typeof theme[key] === 'string');
    const derived = Object.keys(fixed);

    for (const [name, source] of sources) {
        const named = statedKeys(source);
        if (!colours.some((key) => named.has(key))) continue;

        const missing = colours.filter((key) => !named.has(key) && !derived.includes(key));
        if (missing.length) {
            warn('theme-palette', `themes/${name}: names ${named.size} colours of its own but leaves ` +
                `${missing.length} to the theme it imports: ` +
                missing.slice(0, 5).join(', ') + (missing.length > 5 ? ', ...' : ''));
        }
    }
}

/**
 * A derived theme is one transform applied to every colour of the theme it is derived from, and the
 * transform has to hand back colours as distinguishable as it was given. What breaks that is a
 * transform that runs its colours off an end of the scale, where the clamp holds them: a map is
 * mostly pale, so lightening it by a fixed amount pressed the background, the minor roads and the
 * landuse fills into one pure white, and the theme derived from that one inherited one pure black.
 * The style still built, every value still named a colour, and the map came out with its roads
 * indistinguishable from the ground they cross.
 *
 * A colour its parent already holds at an end stays there and is not reported, and a channel within
 * a step of one counts as already there, that step being rounding rather than something an eye
 * separates. Nor is a colour the theme names itself: a palette that asks for white is answering for
 * that colour, where a transform reaches every colour of a map its author never enumerated.
 */
async function checkThemeClamping() {
    const names = readdirSync(join(root, 'themes')).filter((file) => file.endsWith('.js')).sort();
    const clamped = (colour) => {
        const {r, g, b} = colour.toRGB();
        return (r >= 254 && g >= 254 && b >= 254) || (r <= 1 && g <= 1 && b <= 1);
    };

    for (const name of names) {
        const source = readFileSync(join(root, 'themes', name), 'utf8');
        // The theme a theme derives from is the one it imports, and `themes/default.js` imports none.
        const parent = source.match(/from\s+['"]\.\/([^'"]+\.js)['"]/)?.[1];
        if (!parent) continue;

        const values = (await import(`./themes/${name}`)).default;
        const parentValues = (await import(`./themes/${parent}`)).default;
        const named = statedKeys(source);
        const lost = Object.keys(values).filter((key) => {
            if (named.has(key)) return false;
            const before = Color.fromString(parentValues[key]);
            const after = Color.fromString(values[key]);
            return before !== null && after !== null && !clamped(before) && clamped(after);
        });

        if (lost.length) {
            error('theme-clamp', `themes/${name}: ${lost.length} colour(s) are driven to an end of the ` +
                `scale that themes/${parent} kept off it, so they can no longer be told apart: ` +
                lost.slice(0, 5).join(', ') + (lost.length > 5 ? ', ...' : ''));
        }
    }
}

/**
 * An `all` holding two equality tests on the same attribute can never match, so
 * the directive is dead and its features fall through to a later rule or vanish.
 */
function checkUnsatisfiableFilters() {
    for (const layer of style.layers) {
        const seen = new Set();
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (node[0] !== 'all') return;
                const values = new Map();
                for (const child of node.slice(1)) {
                    if (Array.isArray(child) && child[0] === '==' &&
                        Array.isArray(child[1]) && child[1][0] === 'get') {
                        const key = child[1][1];
                        if (!values.has(key)) values.set(key, new Set());
                        values.get(key).add(child[2]);
                    }
                }
                for (const [key, set] of values) {
                    if (set.size < 2) continue;
                    const message = `${layer.id}: filter requires ${key} to equal ` +
                        `${[...set].map((v) => JSON.stringify(v)).join(' and ')} at once, so it never matches`;
                    if (!seen.has(message)) {
                        seen.add(message);
                        error('filter', message);
                    }
                }
            });
        }
    }
}

/**
 * The deprecated filter syntax, `['==', 'power', 'plant']`, names its attribute
 * as a bare string where the expression syntax nests a `get`. MapLibre accepts
 * both, so the map renders either way and nothing here would notice.
 *
 * Everything that reads the style as data does notice. An analyzer walking the
 * expression tree sees a comparison of two literals, not an attribute read, and
 * concludes the layer needs no attributes at all. That is the wrong direction to
 * be wrong in: it yields a tileset that omits the tags the layer filters on, and
 * the layer renders nothing. The `power` and `tourism` layers were filtered
 * exclusively this way and would have been stripped whole.
 *
 * `['has', key]` is spelled the same in both syntaxes and is not affected.
 */
function checkLegacyFilters() {
    const comparisons = new Set(['==', '!=', '<', '<=', '>', '>=', 'in', '!in']);
    for (const layer of style.layers) {
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (!comparisons.has(node[0]) || typeof node[1] !== 'string') return;
                const suggestion = node[1].startsWith('$')
                    ? `['${node[0]}', ['geometry-type'], ...]`
                    : `['${node[0]}', ['get', '${node[1]}'], ...]`;
                error('legacy-filter',
                    `${layer.id}: ${JSON.stringify(node)} uses the deprecated filter syntax; ` +
                    `write it as ${suggestion}`);
            });
        }
    }
}

/**
 * The tests a compiled chain makes, in the order it makes them.
 *
 * A directive list compiles to a `case` of filters, a `match` of values, or the two nested inside
 * one another where the list mixes the shapes. All three answer with their first hit and fall
 * through to what follows, so reading them as one ordered list is what says which directive a
 * feature reaches.
 */
function claims(node, found = []) {
    if (!Array.isArray(node)) return found;
    if (node[0] === 'match' && Array.isArray(node[1]) && node[1][0] === 'get') {
        for (let i = 2; i < node.length - 1; i += 2) {
            for (const value of [node[i]].flat()) found.push(`${node[1][1]}=${value}`);
        }
        claims(node[node.length - 1], found);
    } else if (node[0] === 'case') {
        for (let i = 1; i < node.length - 1; i += 2) {
            found.push(JSON.stringify(node[i]));
        }
        claims(node[node.length - 1], found);
    }
    return found;
}

/**
 * The ways a feature can satisfy a test, each as the attribute values it would have to carry.
 *
 * A test is read as a disjunction of conjunctions, which is the shape a directive filter takes: an
 * `any` offers alternatives, an `all` combines them, and an `==` pins one attribute. Anything else,
 * a negation above all, constrains a feature without pinning an attribute and asks for nothing
 * here, which is the safe direction: a test read as asking for less is never reported.
 */
function alternatives(filter) {
    const nothing = [new Set()];
    if (!Array.isArray(filter)) return nothing;
    const [operator, left, right] = filter;
    if (operator === 'any') {
        return filter.slice(1).flatMap(alternatives);
    }
    if (operator === 'all') {
        let combined = nothing;
        for (const child of filter.slice(1)) {
            const next = [];
            for (const one of combined) {
                for (const other of alternatives(child)) {
                    next.push(new Set([...one, ...other]));
                }
            }
            // A filter whose alternatives multiply out of hand is left saying nothing rather than
            // enumerated, the check being an aid and not a solver.
            if (next.length > 32) return nothing;
            combined = next;
        }
        return combined;
    }
    if (operator === '==' && Array.isArray(left) && left[0] === 'get' &&
        (typeof right === 'string' || typeof right === 'number')) {
        return [new Set([`${left[1]}=${right}`])];
    }
    return nothing;
}

/** Whether every way of satisfying the second test already satisfies the first. */
function covers(earlier, later) {
    if (earlier.length === 0 || earlier.some((one) => one.size === 0)) return false;
    return later.every((one) => earlier.some((other) => [...other].every((want) => one.has(want))));
}

/**
 * A directive that asks for everything an earlier one asks for, and more, is the narrower of the
 * two and comes second, so the chain answers with the earlier one and the narrower never draws.
 *
 * This is how a special case is lost: the tunnel colour of a track, written below the directive
 * that gives every track the colour it has in the open, or the lattice tower written below the one
 * that gives every tower the generic tower icon. Both leave a theme colour or a sprite that nothing
 * reaches, which nothing else reports, because each directive is well formed on its own.
 */
function checkNarrowedDirectives() {
    for (const layer of style.layers) {
        const reported = new Set();
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (node[0] !== 'case' && node[0] !== 'match') return;
                const seen = [];
                for (const claim of claims(node)) {
                    const wants = claim.startsWith('[')
                        ? alternatives(JSON.parse(claim))
                        : [new Set([claim])];
                    for (const earlier of seen) {
                        if (!covers(earlier.wants, wants)) continue;
                        const message = `${layer.id}: a directive matched by ` +
                            `${describe(wants)} is written below one matched by ` +
                            `${describe(earlier.wants)}, which answers first`;
                        if (!reported.has(message)) {
                            reported.add(message);
                            warn('narrowed', message);
                        }
                    }
                    seen.push({wants});
                }
            });
        }
    }
}

/** Name a set of alternatives the way the directive that produced it reads. */
function describe(wants) {
    return wants.map((one) => [...one].sort().join(' and ')).join(' or ');
}

/**
 * A chain answers with its first hit, so a test repeated further down it is dead. Two directives
 * for one feature class usually means an edit landed in the wrong group rather than that the
 * second was meant to lose.
 */
function checkShadowedDirectives() {
    for (const layer of style.layers) {
        const reported = new Set();
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (node[0] !== 'case' && node[0] !== 'match') return;
                const seen = new Map();
                let position = 0;
                for (const claim of claims(node)) {
                    position += 1;
                    if (!seen.has(claim)) {
                        seen.set(claim, position);
                        continue;
                    }
                    const message = `${layer.id}: directive ${position} repeats the test of ` +
                        `directive ${seen.get(claim)} and can never apply: ${claim}`;
                    if (!reported.has(message)) {
                        reported.add(message);
                        error('shadowed', message);
                    }
                }
            });
        }
    }
}

/** Collect the attribute values a compiled layer tests for. */
function coverage(layer, attribute) {
    const values = new Set();
    for (const expression of expressionsOf(layer)) {
        walk(expression, (node) => {
            const [op, left, right] = node;
            if (!Array.isArray(left) || left[0] !== 'get' || left[1] !== attribute) return;
            if (op === '==' && typeof right === 'string') {
                values.add(right);
            } else if (op === 'in' && Array.isArray(right) && right[0] === 'literal') {
                for (const value of right[1]) values.add(value);
            } else if (op === 'match') {
                // A branch label is one value or the list of values that share the branch.
                for (let i = 2; i < node.length - 1; i += 2) {
                    for (const value of [node[i]].flat()) values.add(value);
                }
            }
        });
    }
    return values;
}

/** Report a feature class handled by some members of a group but not others. */
function checkParity() {
    const byId = new Map(style.layers.map((layer) => [layer.id, layer]));

    for (const group of parityGroups) {
        const present = group.layers.filter((id) => byId.has(id));
        if (present.length !== group.layers.length) {
            const missing = group.layers.filter((id) => !byId.has(id));
            warn('parity', `${group.name}: no such layer in the style: ${missing.join(', ')}`);
        }
        if (present.length < 2) continue;

        const covered = new Map(present.map((id) => [id, coverage(byId.get(id), group.attribute)]));
        const union = new Set([...covered.values()].flatMap((set) => [...set]));
        const accepted = new Set(group.accept);

        for (const value of [...union].sort()) {
            const missing = present.filter((id) => !covered.get(id).has(value));
            if (!missing.length) continue;
            const key = `${value}:${missing.join(',')}`;
            if (accepted.has(value) || accepted.has(key)) continue;
            warn('parity', `${group.name}: ${group.attribute}=${value} is styled by ` +
                `${present.filter((id) => covered.get(id).has(value)).join(', ')} ` +
                `but not by ${missing.join(', ')}`);
        }
    }
}

/**
 * A layer's id is the name it is known by everywhere: to `setPaintProperty` and `moveLayer`, to a
 * `beforeId` someone inserts their own layer at, and to anyone reading a tile in a debugger. So it
 * is fixed rather than chosen, and it is the module that defines it, `<topic>_<name>` read off the
 * path, or the topic alone where the module is the topic's only one and is named `style.js`.
 *
 * The ids drifted from that once already, leaving `tunnel_line` and `bridge_line` for two layers
 * that draw highways, `icon` for one that draws points, and `amenity_fill_2` for the fountains.
 * Nothing said which module to open for any of them.
 */
function checkLayerNames(files) {
    const ids = new Map();
    for (const file of files) {
        const source = readFileSync(join(root, file), 'utf8');
        for (const match of source.matchAll(/^\s*['"]?id['"]?:\s*['"]([^'"]+)['"]/gm)) {
            ids.set(match[1], file);
        }
    }
    for (const [id, file] of ids) {
        const [, topic, module] = file.match(/^layers\/([^/]+)\/(.+)\.js$/);
        const wanted = module === 'style' ? topic : `${topic}_${module}`;
        if (id !== wanted) {
            error('layer-name', `${file} defines '${id}', but a module at that path is named ` +
                `'${wanted}'. Rename the layer, or the module if the layer is better named.`);
        }
    }
}

/**
 * Properties whose value is what the renderer would have done anyway.
 *
 * A style is read to find out what someone decided, so a property that restates a default is a
 * decision that was never made, sitting where one would be looked for. `visibility: 'visible'`
 * stood on all fifty-seven layers.
 */
const defaults = {
    visibility: 'visible',
    'fill-antialias': true,
    'icon-size': 1,
    'icon-opacity': 1,
    'text-opacity': 1,
    'line-opacity': 1,
    'fill-opacity': 1,
    'icon-allow-overlap': false,
    'text-allow-overlap': false,
};

/*
 * `line-cap`, `line-join` and `symbol-placement` are deliberately not in that table. Their defaults
 * are `butt`, `miter` and `point`, and a layer writing one of those down is saying it differs from
 * the layer beside it: the bridges are square-ended where the roads they carry are round. Removing
 * the words would leave the difference true and unstated.
 */

function checkDefaults() {
    for (const layer of style.layers) {
        if (layer.minzoom === 0) {
            warn('default', `${layer.id}: minzoom 0 is the zoom a layer starts at anyway`);
        }
        if (layer.maxzoom === 24) {
            warn('default', `${layer.id}: maxzoom 24 is the zoom a layer ends at anyway`);
        }
        for (const section of ['layout', 'paint']) {
            for (const [property, value] of Object.entries(layer[section] ?? {})) {
                if (property in defaults && value === defaults[property]) {
                    error('default', `${layer.id}: ${property} is set to ${JSON.stringify(value)}, ` +
                        `which is what it is when nothing sets it`);
                }
            }
        }
    }
}

/**
 * A symbol layer drawing more than one class of thing decides which of two labels survives where
 * they collide, and decides it with `symbol-sort-key`. Without one the winner is whichever feature
 * the tile happens to hold first, which changes between tiles and between imports, so a label
 * appears and disappears as the map is panned.
 */
function checkSymbolSortKeys() {
    for (const layer of style.layers) {
        if (layer.type !== 'symbol') continue;
        const classes = claims(layer.paint?.['text-color'] ?? layer.layout?.['icon-image'] ?? []).length;
        if (classes > 1 && layer.layout?.['symbol-sort-key'] === undefined) {
            error('symbol-sort-key', `${layer.id}: draws ${classes} classes of symbol and gives no ` +
                `symbol-sort-key, so which one survives a collision is undecided`);
        }
    }
}

/**
 * How far a chain is walked before it answers.
 *
 * A `case` is a linear scan, so a feature is tested against every branch above the one that holds
 * its answer. A `match` is a lookup and costs the same whatever it holds, so only the `case` links
 * are counted. The icon layer was one `case` of two hundred and fifty six.
 */
function checkChainLength() {
    const limit = 12;
    for (const layer of style.layers) {
        let longest = 0;
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (node[0] !== 'case') return;
                longest = Math.max(longest, (node.length - 2) / 2);
            });
        }
        if (longest > limit) {
            warn('chain', `${layer.id}: a case runs to ${longest} branches, each one a comparison ` +
                `made before the next. Tests on one attribute belong in a match, which is a lookup`);
        }
    }
}

/** A layer module that nothing imports is dead weight and drifts unnoticed. */
function checkOrphans(files) {
    // A layer module is reached through the topic that owns it, and a topic imports its layers by a
    // path relative to itself, so each module's imports are resolved against its own directory.
    const imported = new Set();
    const scan = (file) => {
        const source = readFileSync(join(root, file), 'utf8');
        for (const match of source.matchAll(/from\s+['"](\.[^'"]+)['"]/g)) {
            imported.add(join(dirname(file), match[1]));
        }
    };
    scan('map.js');

    for (const file of files) {
        if (imported.has(file) || kept[file]) continue;
        warn('orphan', `${file} is never imported by map.js`);
    }
    for (const file of Object.keys(kept)) {
        if (imported.has(file)) {
            warn('orphan', `${file} is listed as deliberately unused but map.js imports it`);
        } else if (!files.includes(file)) {
            warn('orphan', `${file} is listed as deliberately unused but does not exist`);
        }
    }
}

/**
 * Modules kept out of the map on purpose, each with the reason. An orphan is normally a layer left
 * behind by an edit, so one that is meant to sit outside the map says so here rather than being
 * reported for as long as it exists.
 */
const kept = {
    'layers/building/extrusion.js':
        'draws buildings in three dimensions, which this map does not; kept for a style that does',
};

function checkDuplicateLayerIds() {
    const seen = new Set();
    for (const layer of style.layers) {
        if (seen.has(layer.id)) error('layer-id', `duplicate layer id: ${layer.id}`);
        seen.add(layer.id);
    }
}

/**
 * Validate against the MapLibre style specification when it is installed.
 * Optional so that the checks above run with no node_modules present.
 */
async function checkSpec() {
    let validateStyleMin;
    try {
        ({validateStyleMin} = await import('@maplibre/maplibre-gl-style-spec'));
    } catch {
        warn('spec', 'skipped: run `npm install` in basemap/ to enable MapLibre spec validation');
        return;
    }
    for (const issue of validateStyleMin(JSON.parse(JSON.stringify(style)))) {
        error('spec', issue.message);
    }
}

/**
 * A source layer is read by several style layers, so exactly one of them says where its features
 * come from. Two answers to that question, or none, is a mistake the compiler refuses; catching it
 * here names the layers involved instead of failing at build time.
 */
function checkSources() {
    const declared = new Map();
    const read = new Set();
    const terrain = map.terrain ? (map.terrain.id ?? 'terrain') : null;
    for (const layer of map.layers) {
        const id = layer.sourceLayer;
        if (!id) continue;
        // A layer reading the terrain reads tiles that are traced from elevation rather than
        // queried, so it neither declares a query nor leaves one owing.
        if (terrain && layer.source === terrain) {
            if (layer.sourceQueries) {
                error('source', `${layer.id}: reads the terrain, so it cannot declare source-queries`);
            }
            continue;
        }
        read.add(id);
        const queries = layer.sourceQueries;
        if (!queries) continue;
        if (declared.has(id)) {
            error('source', `${id}: declared by both ${declared.get(id)} and ${layer.id}`);
        }
        declared.set(id, layer.id);
        for (const query of queries) {
            if (!query.from) {
                error('source', `${id}: a query in ${layer.id} does not name a relation`);
            }
            if (!(query.minzoom < query.maxzoom)) {
                error('source', `${id}: a query in ${layer.id} covers no zoom level ` +
                    `(minzoom ${query.minzoom}, maxzoom ${query.maxzoom})`);
            }
        }
        const schema = layer.sourceSchema;
        if (schema && !existsSync(join(root, schema))) {
            error('source', `${id}: source-schema names ${schema}, which does not exist`);
        }
    }
    for (const id of read) {
        if (!declared.has(id)) {
            error('source', `${id}: no layer says where it comes from`);
        }
    }
}

// --- report ----------------------------------------------------------------

const files = layerFiles();
checkThemeReferences(files);
await checkThemeValues();
checkThemePalettes(themeFiles());
await checkThemeClamping();
checkUnsatisfiableFilters();
checkLegacyFilters();
checkShadowedDirectives();
checkNarrowedDirectives();
checkParity();
checkOrphans(files);
checkLayerNames(files);
checkDefaults();
checkSymbolSortKeys();
checkChainLength();
checkDuplicateLayerIds();
checkSources();
await checkSpec();

for (const {check, message} of warnings) console.warn(`warning  [${check}] ${message}`);
for (const {check, message} of errors) console.error(`error    [${check}] ${message}`);

console.log(`\n${style.layers.length} layers, ${Object.keys(theme).length} theme keys, ` +
    `${(JSON.stringify(style).length / 1024).toFixed(0)} KB compiled`);
console.log(`${errors.length} error(s), ${warnings.length} warning(s)`);

process.exit(errors.length ? 1 : 0);
