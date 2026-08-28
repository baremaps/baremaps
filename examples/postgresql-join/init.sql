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
    EXTENSION IF NOT EXISTS postgis;

-- Drop and create schema
DROP
    SCHEMA IF EXISTS baremaps CASCADE;

CREATE
    SCHEMA baremaps;

-- Create table baremaps.tree_type
CREATE
    TABLE
        IF NOT EXISTS baremaps.tree_type(
            id SERIAL PRIMARY KEY,
            TYPE VARCHAR(255) NOT NULL
        );

-- Populate baremaps.tree_type with 10 different types of trees
INSERT
    INTO
        baremaps.tree_type(TYPE)
    VALUES('Oak'),
    ('Maple'),
    ('Pine'),
    ('Birch'),
    ('Spruce'),
    ('Willow'),
    ('Cherry'),
    ('Poplar'),
    ('Cypress'),
    ('Cedar');

-- Create table baremaps.point_trees
CREATE
    TABLE
        IF NOT EXISTS baremaps.point_trees(
            id SERIAL PRIMARY KEY,
            geom GEOMETRY(
                Point,
                3857
            ) NOT NULL,
            SIZE INTEGER,
            tree_type_id INTEGER REFERENCES baremaps.tree_type(id)
        );

-- Populate baremaps.point_trees with 1000 random points in France
INSERT
    INTO
        baremaps.point_trees(
            geom,
            SIZE,
            tree_type_id
        ) SELECT
            ST_Transform(
                ST_SetSRID(
                    ST_MakePoint(
                        random()*(
                            5.5 - 0.5
                        )+ 0.5, -- Longitude range for France
                        random()*(
                            51.1 - 41.1
                        )+ 41.1 -- Latitude range for France

                    ),
                    4326
                ),
                3857
            ),
            FLOOR( random()* 100 )+ 1, -- Random size between 1 and 100FLOOR( random()* 10 )+ 1 -- Random tree_type_id between 1 and 10
        FROM
            generate_series(
                1,
                1000
            );

-- Number of points to generate

