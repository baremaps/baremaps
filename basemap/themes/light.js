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

import style from './grayscale.js';
import {Color} from "../utils/color.js";

/**
 * A label is not lightened with the map it lies on.
 *
 * The lightening is what makes this theme lighter than the grey one it is built from, and it is
 * meant for the ground: the fills, the roads, the water. Applied to a label as well it lightens
 * the ink, which is the opposite of what a lighter map wants, and it took eight of them under the
 * contrast a reader needs for text this size. A halo is left alone for the same reason
 * `transportation.js` leaves it alone, being neither ground nor ink but what holds them apart.
 */
const ink = (key) => /text|label/i.test(key) && !/halo/i.test(key);

export default Object.entries(style).reduce((acc, [key, value]) => {
    let color = Color.fromString(value);
    if (color == null) {
        acc[key] = value;
        return acc;
    } else if (ink(key)) {
        acc[key] = color.grayscale().toString();
        return acc;
    } else {
        acc[key] = color.grayscale().lighten(0.1).toString();
        return acc;
    }
}, {});
