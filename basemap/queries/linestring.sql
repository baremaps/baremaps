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
CREATE
    OR REPLACE VIEW osm_linestring AS SELECT
        id,
        tags,
        geom
    FROM
        osm_way
    LEFT JOIN osm_member ON
        id = member_ref
    WHERE
        geom IS NOT NULL
        AND ST_GeometryType(osm_way.geom)= 'ST_LineString'
        AND tags != '{}'
        AND member_ref IS NULL
UNION SELECT
        id,
        tags,
        geom
    FROM
        osm_relation
    WHERE
        geom IS NOT NULL
        AND ST_GeometryType(osm_relation.geom)= 'ST_LineString'
        AND tags != '{}'
UNION SELECT
        id,
        tags,
        (
            st_dump(geom)
        ).geom AS geom
    FROM
        osm_relation
    WHERE
        geom IS NOT NULL
        AND ST_GeometryType(osm_relation.geom)= 'ST_MultiLineString'
        AND tags != '{}';