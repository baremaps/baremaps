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
    TABLE
        IF NOT EXISTS osm_header(
            replication_sequence_number BIGINT PRIMARY KEY,
            replication_timestamp TIMESTAMP WITHOUT TIME ZONE,
            replication_url text,
            SOURCE text,
            writing_program text
        );

CREATE
    TABLE
        osm_node(
            id BIGINT PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset BIGINT,
            tags jsonb,
            lon FLOAT,
            lat FLOAT,
            geom geometry(point)
        );

CREATE
    TABLE
        osm_way(
            id BIGINT PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset BIGINT,
            tags jsonb,
            nodes BIGINT [],
            geom geometry
        );

CREATE
    TABLE
        osm_relation(
            id BIGINT PRIMARY KEY,
            version INT,
            uid INT,
            TIMESTAMP TIMESTAMP WITHOUT TIME ZONE,
            changeset BIGINT,
            tags jsonb,
            member_refs BIGINT [],
            member_types INT [],
            member_roles text [],
            geom geometry
        );