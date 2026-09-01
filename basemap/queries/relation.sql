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
DROP
    TABLE
        IF EXISTS osm_relation CASCADE;

CREATE
    TABLE
        IF NOT EXISTS osm_relation(
            id int8 PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset int8,
            tags jsonb,
            member_refs BIGINT [],
            member_types INT [],
            member_roles text [],
            geom geometry
        );

DROP
    INDEX IF EXISTS osm_relation_geom_index;

DROP
    INDEX IF EXISTS osm_relation_tags_index;

CREATE
    INDEX IF NOT EXISTS osm_relation_geom_index ON
    osm_relation
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_relation_tags_index ON
    osm_relation
        USING GIN(tags);
