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
        IF EXISTS water_polygons_shp CASCADE;

CREATE
    TABLE
        IF NOT EXISTS water_polygons_shp(
            x BIGINT,
            y BIGINT,
            geometry geometry
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_ocean CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_ocean AS SELECT
        ROW_NUMBER() OVER() AS id,
        '{"ocean":"water"}'::jsonb AS tags,
        st_setsrid(
            geometry,
            3857
        ) AS geom
    FROM
        water_polygons_shp;

DROP
    INDEX IF EXISTS osm_ocean_geometry_index;

CREATE
    INDEX IF NOT EXISTS osm_ocean_geometry_index ON
    osm_ocean
        USING gist(geom);

DROP
    TABLE
        IF EXISTS simplified_water_polygons_shp CASCADE;

CREATE
    TABLE
        IF NOT EXISTS simplified_water_polygons_shp(
            x BIGINT,
            y BIGINT,
            geometry geometry
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_ocean_simplified CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_ocean_simplified AS SELECT
        ROW_NUMBER() OVER() AS id,
        '{"ocean":"water"}'::jsonb AS tags,
        st_setsrid(
            geometry,
            3857
        ) AS geom
    FROM
        simplified_water_polygons_shp;

DROP
    INDEX IF EXISTS osm_ocean_simplified_geometry_index;

CREATE
    INDEX IF NOT EXISTS osm_ocean_simplified_geometry_index ON
    osm_ocean_simplified
        USING gist(geom);