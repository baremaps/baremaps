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
import {readFileSync, readdirSync, statSync} from 'fs';
import {join, dirname} from 'path';
import {fileURLToPath} from 'url';

import theme from './theme.js';
import style from './style.js';
import {Color} from './utils/color.js';

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
        layers: ['highway_line', 'tunnel_line', 'bridge_line'],
        accept: [],
    },
    {
        name: 'highway outline',
        attribute: 'highway',
        layers: ['highway_outline', 'tunnel_outline', 'bridge_outline'],
        accept: [],
    },
];

// --- helpers ---------------------------------------------------------------

function layerFiles() {
    const files = [];
    for (const dir of readdirSync(join(root, 'layers'))) {
        const path = join(root, 'layers', dir);
        if (!statSync(path).isDirectory()) continue;
        for (const file of readdirSync(path)) {
            if (file.endsWith('.js') && file !== 'tileset.js') {
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
 * A `case` returns its first matching branch, so a filter repeated later in the
 * same chain is dead. Two directives for one feature class usually means an
 * edit landed in the wrong group rather than that the second was meant to lose.
 */
function checkShadowedDirectives() {
    for (const layer of style.layers) {
        const reported = new Set();
        for (const expression of expressionsOf(layer)) {
            walk(expression, (node) => {
                if (node[0] !== 'case') return;
                const seen = new Map();
                for (let i = 1; i < node.length - 1; i += 2) {
                    const filter = JSON.stringify(node[i]);
                    const position = (i + 1) / 2;
                    if (!seen.has(filter)) {
                        seen.set(filter, position);
                        continue;
                    }
                    const message = `${layer.id}: directive ${position} repeats the filter of ` +
                        `directive ${seen.get(filter)} and can never apply: ${filter}`;
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

/** A layer module that nothing imports is dead weight and drifts unnoticed. */
function checkOrphans(files) {
    const imports = readFileSync(join(root, 'style.js'), 'utf8') +
        readFileSync(join(root, 'tileset.js'), 'utf8');
    for (const file of files) {
        if (!imports.includes(file.replace('layers/', ''))) {
            warn('orphan', `${file} is never imported by style.js or tileset.js`);
        }
    }
}

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

// --- report ----------------------------------------------------------------

const files = layerFiles();
checkThemeReferences(files);
await checkThemeValues();
checkUnsatisfiableFilters();
checkShadowedDirectives();
checkParity();
checkOrphans(files);
checkDuplicateLayerIds();
await checkSpec();

for (const {check, message} of warnings) console.warn(`warning  [${check}] ${message}`);
for (const {check, message} of errors) console.error(`error    [${check}] ${message}`);

console.log(`\n${style.layers.length} layers, ${Object.keys(theme).length} theme keys, ` +
    `${(JSON.stringify(style).length / 1024).toFixed(0)} KB compiled`);
console.log(`${errors.length} error(s), ${warnings.length} warning(s)`);

process.exit(errors.length ? 1 : 0);
