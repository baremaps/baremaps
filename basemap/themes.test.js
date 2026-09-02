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
import {join, dirname} from 'path';

import defaultTheme from './themes/default.js';
import {render, NAMES} from './generate.js';
import {lostDistinctions, DICHROMACY, ANOMALY} from './utils/vision.js';

const root = dirname(fileURLToPath(import.meta.url));

/**
 * The eight palettes drawn for a reader with a colour vision deficiency are written out rather than
 * worked out when the style is built, so they can fall behind the palette they are written from.
 * A colour added to `themes/default.js`, or one moved in it, and they are stale: the style still
 * builds, every value still names a colour, and a reader is handed a palette measured against a map
 * that has changed underneath it. `npm run themes` writes them again.
 */
test('the generated palettes are the ones the generator writes', () => {
    for (const name of NAMES) {
        const path = join(root, 'themes', `${name}.js`);
        assert.equal(readFileSync(path, 'utf8'), render(name),
            `themes/${name}.js is not what generate.js writes. Run \`npm run themes\`.`);
    }
});

/**
 * And that what they are written for actually holds: each leaves its reader with no more
 * distinctions lost than the palette they would have had otherwise. `adaptPalette` guarantees this
 * of what it returns; this checks the file that was written from it.
 */
test('each palette leaves its reader no worse off than the default one', async () => {
    const conditions = [
        ['protanopia', 'protan', DICHROMACY], ['protanomaly', 'protan', ANOMALY],
        ['deuteranopia', 'deutan', DICHROMACY], ['deuteranomaly', 'deutan', ANOMALY],
        ['tritanopia', 'tritan', DICHROMACY], ['tritanomaly', 'tritan', ANOMALY],
    ];
    for (const [name, type, severity] of conditions) {
        const palette = (await import(`./themes/${name}.js`)).default;
        const drawn = lostDistinctions(palette, defaultTheme, type, severity);
        const otherwise = lostDistinctions(defaultTheme, defaultTheme, type, severity);
        assert.ok(drawn <= otherwise,
            `themes/${name}.js loses ${drawn} distinctions where the default palette loses ${otherwise}`);
    }
});
