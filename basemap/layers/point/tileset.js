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
export default {
    "id": "point",
    "queries": [
        {
            "minzoom": 1,
            "maxzoom": 2,
            "sql": "SELECT id, jsonb_build_object('name', tags -> 'name', 'population', tags -> 'population', 'place', tags -> 'place') as tags, geom FROM osm_point_z$zoom WHERE tags != '{}' AND (tags ->> 'place' = 'country')"
        },
        {
            "minzoom": 2,
            "maxzoom": 8,
            "sql": "SELECT id, jsonb_build_object('name', tags -> 'name', 'population', tags -> 'population', 'place', tags -> 'place', 'capital', tags -> 'capital') as tags, geom FROM osm_point_z$zoom WHERE tags != '{}'"
        },
        {
            "minzoom": 8,
            "maxzoom": 20,
            "sql": "SELECT id, tags, geom FROM osm_point_z$zoom WHERE tags != '{}'"
        }
    ]
}
