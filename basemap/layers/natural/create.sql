-- Licensed under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- The features themselves. The levels below zoom 13 are generalized from these, and the
-- engine builds them; see the generalize member of the layer that reads this source.
CREATE
    OR REPLACE VIEW osm_natural AS SELECT
        id,
        tags,
        geom
    FROM
        osm_way
    WHERE
        tags ? 'natural'
        AND NOT EXISTS(
            SELECT
                1
            FROM
                osm_member_tag
            WHERE
                member_ref = osm_way.id
                AND tag_key = 'natural'
        )
UNION SELECT
        id,
        tags,
        geom
    FROM
        osm_relation
    WHERE
        tags ? 'natural';
