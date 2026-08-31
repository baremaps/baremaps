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
 * that working without relying on dynamic import or top-level await. Building
 * every theme costs a few thousand colour conversions, which is not measurable
 * against the rest of the build.
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

export default themes[name];
