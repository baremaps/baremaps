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
import theme from "../../theme.js";


export default {
    "id": "power_tower",
    "type": "circle",
    "filter": [
        "any",
        ["==", "power", "tower"],
        ["==", "power", "pole"],
        ["==", "power", "portal"],
        ["==", "power", "catenary_mast"]
    ],
    "source": "baremaps",
    "source-layer": "point",
    "layout": {
        "visibility": "visible"
    },
    "paint": {
        'circle-pitch-alignment': 'map',
        "circle-color": theme.powerTowerCircleColor,
        "circle-radius": [
            "interpolate",
            ["exponential", 1],
            ["zoom"],
            14, 1,
            20, 8
        ]
    }
}
