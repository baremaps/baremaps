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

import style from './default.js';
import {Color} from "../utils/color.js";

/**
 * The theme picks the network out by the name of the key, which is the only thing that says what a
 * colour is for. A halo is the exception: it is not the colour of the thing it surrounds, it is
 * what holds a label off the map behind it, and every halo of the style is the same white for that
 * reason. `highwayLabelHaloColor` is filed under a road without being one, so darkening it with
 * the network left the road labels the only ones ringed in grey, one of nine.
 */
function emphasized(key) {
    const name = key.toLowerCase();
    if (name.includes("halo")) {
        return false;
    }
    return name.includes("highway")
        || name.includes("tunnel")
        || name.includes("bridge")
        || name.includes("rail")
        || name.includes("ferry");
}

export default Object.entries(style).reduce((acc, [key, value]) => {
    let color = Color.fromString(value);
    if (color == null) {
        acc[key] = value;
        return acc;
    } else if (emphasized(key)) {
        acc[key] = color.darken(0.2).toString();
        return acc;
    } else {
        acc[key] = color.grayscale().lighten(0.1).toString();
        return acc;
    }
}, {});