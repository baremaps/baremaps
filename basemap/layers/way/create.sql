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
        IF EXISTS osm_way CASCADE;

CREATE
    TABLE
        IF NOT EXISTS osm_way(
            id int8 PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset int8,
            tags jsonb,
            nodes int8 [],
            geom geometry
        );

DROP
    INDEX IF EXISTS osm_way_geom_index;

DROP
    INDEX IF EXISTS osm_way_tags_index;

CREATE
    INDEX IF NOT EXISTS osm_way_geom_index ON
    osm_way
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_way_tags_index ON
    osm_way
        USING GIN(tags);
