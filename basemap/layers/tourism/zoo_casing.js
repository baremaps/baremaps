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
    "id": "tourism_zoo_casing",
    "type": "line",
    "filter": ["all", ["==", ["get", "tourism"], "zoo"]],
    sourceLayer: "tourism",
    sourceQueries: [
        {"minzoom": 14, "maxzoom": 20, "from": "osm_tourism"},
    ],
    "layout": {
        "visibility": "visible"
    },
    "paint": {
        "line-color": theme.tourismZooCasingLineColor,
        "line-width": [
            "interpolate", ["exponential", 1.2], ["zoom"], 13, 3, 16,
            3, 19, 10
        ]
    }
}
