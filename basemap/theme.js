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
 * Selects the theme the style is built against.
 *
 * Set `BAREMAPS_THEME` to the name of a file in `themes/` to build with a
 * different palette, for example:
 *
 *     BAREMAPS_THEME=dark baremaps map dev --tileset tileset.js --style style.js
 *
 * The themes are imported statically rather than resolved on demand: Baremaps
 * evaluates this file as an ES module through GraalJS, and a static graph keeps
 * that working without relying on dynamic import or top-level await. Every theme
 * is therefore built, whichever one is asked for, so each has to be cheap. The
 * ones that are not are the eight drawn for a reader with a colour vision
 * deficiency, each of which measures the whole palette against a model of that
 * reader's eye; those are written out by `generate.js` and are read here rather
 * than worked out. The rest are a transform of a colour at a time, which is a
 * few thousand conversions and not measurable against the rest of the build.
 */
import achromatomaly from './themes/achromatomaly.js';
import achromatopsia from './themes/achromatopsia.js';
import contrast from './themes/contrast.js';
import dark from './themes/dark.js';
import defaultTheme from './themes/default.js';
import deuteranomaly from './themes/deuteranomaly.js';
import deuteranopia from './themes/deuteranopia.js';
import grayscale from './themes/grayscale.js';
import light from './themes/light.js';
import protanomaly from './themes/protanomaly.js';
import protanopia from './themes/protanopia.js';
import soft from './themes/soft.js';
import transportation from './themes/transportation.js';
import tritanomaly from './themes/tritanomaly.js';
import tritanopia from './themes/tritanopia.js';

const themes = {
    achromatomaly,
    achromatopsia,
    contrast,
    dark,
    default: defaultTheme,
    deuteranomaly,
    deuteranopia,
    grayscale,
    light,
    protanomaly,
    protanopia,
    soft,
    transportation,
    tritanomaly,
    tritanopia,
};

/**
 * Baremaps exposes the process environment to the script as a global `env`
 * object (see ConfigReader); Node, which runs validate.js, exposes it as
 * `process.env`. Read whichever is present so that both build the same style.
 */
function selected() {
    if (typeof env !== 'undefined' && env && env.BAREMAPS_THEME) {
        return env.BAREMAPS_THEME;
    }
    if (typeof process !== 'undefined' && process.env && process.env.BAREMAPS_THEME) {
        return process.env.BAREMAPS_THEME;
    }
    return 'default';
}

const name = selected();

if (!Object.prototype.hasOwnProperty.call(themes, name)) {
    throw new Error(
        `Unknown theme '${name}'. Set BAREMAPS_THEME to one of: ${Object.keys(themes).sort().join(', ')}.`,
    );
}

/**
 * The colours no theme derives, taken from the default one whichever theme is selected.
 *
 * Every other theme is the default put through a transform of each colour, which is what a colour
 * ought to get: a dark map wants its land, its water and its roads inverted. The hillshade colours
 * are not the colour of anything, they are the light falling on it, and inverting a map does not
 * move the sun. Deriving them turns the lit slopes dark and the shaded ones light, which reads as
 * terrain pressed into the ground rather than standing out of it. White adds light and a neutral
 * dark adds shadow whatever they are drawn over, so they hold across every theme unchanged.
 */
export const fixed = Object.fromEntries(Object.entries(defaultTheme)
    .filter(([key]) => key.startsWith('terrainHillshade')));

export default {...themes[name], ...fixed};
