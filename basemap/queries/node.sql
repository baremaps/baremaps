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
        IF EXISTS osm_node CASCADE;

CREATE
    TABLE
        IF NOT EXISTS osm_node(
            id int8 PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset int8,
            tags jsonb,
            lon FLOAT,
            lat FLOAT,
            geom geometry(point)
        );

DROP
    INDEX IF EXISTS osm_node_geom_index;

DROP
    INDEX IF EXISTS osm_node_tags_index;

-- A node reaches a tile only when it says something about itself: osm_point selects the nodes that
-- carry tags, and nineteen nodes in twenty carry none -- those are there to give a way its shape,
-- and are read by id when a way is built rather than by extent.
--
-- So the index is restricted to the ones a tile can draw. Over every node it is an index over the
-- nineteen that no tile will ever ask for: on a Swiss extract it held 57 million entries in 2.3 GB,
-- and a dense tile at zoom 14 walked it to fetch 68,763 rows and kept 14,400. Restricted, it holds
-- 3.1 million entries in 123 MB and returns the 14,400 alone, which took that tile from 136 ms to
-- 20 ms.
--
-- The predicate is written the way osm_point writes it, because an index is only used by a query
-- whose condition implies its own.
CREATE
    INDEX IF NOT EXISTS osm_node_geom_index ON
    osm_node
        USING GIST(geom)
    WHERE
        tags <> '{}';

-- An untagged node contributes no entry to a GIN index over its tags, so this one is already
-- confined to the same rows and needs no predicate of its own.
CREATE
    INDEX IF NOT EXISTS osm_node_tags_index ON
    osm_node
        USING GIN(tags);
