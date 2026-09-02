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

-- One view per source layer: where each layer's features come from.
--
-- A layer names one of these in its `sourceQueries`, and a generalized layer's chain of zoom
-- levels is named after it too, so the name is the handle rather than an alias. Most read the base
-- tables and nothing else, so the order below is alphabetical rather than a dependency. The
-- buildings at the end are the exception: a building is coloured by the land it stands on, so it
-- reads the landuse, and the two are therefore written in the order they have to be created in.
--
-- Most are one filter on osm_way, and are kept side by side rather than each in its own file so
-- that a subject styled differently from its neighbours is visible instead of buried.

CREATE OR REPLACE VIEW osm_aerialway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'aerialway';

CREATE OR REPLACE VIEW osm_aeroway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'aeroway';

CREATE OR REPLACE VIEW osm_attraction AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'attraction';

CREATE OR REPLACE VIEW osm_barrier AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'barrier';

CREATE OR REPLACE VIEW osm_boundary AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'boundary';

CREATE OR REPLACE VIEW osm_highway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'highway';

CREATE OR REPLACE VIEW osm_man_made AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'man_made';

CREATE OR REPLACE VIEW osm_power AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL
  AND tags ->> 'power' IN ('cable', 'line', 'minor_line', 'plant', 'substation');

CREATE OR REPLACE VIEW osm_railway AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'railway';

CREATE OR REPLACE VIEW osm_waterway AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'waterway';

CREATE OR REPLACE VIEW osm_route AS
SELECT id, tags, geom FROM osm_linestring
WHERE tags ? 'route';

-- An area can be tagged on the way that outlines it or on the multipolygon that collects those
-- ways, so these read both. A way whose relation already carries the tag is dropped, because the
-- relation is the same area and drawing both draws it twice.

CREATE OR REPLACE VIEW osm_amenity AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'amenity'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'amenity')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE tags ? 'amenity';

CREATE OR REPLACE VIEW osm_landuse AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'landuse'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'landuse')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE tags ? 'landuse';

CREATE OR REPLACE VIEW osm_leisure AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'leisure'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'leisure')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE tags ? 'leisure';

CREATE OR REPLACE VIEW osm_natural AS
SELECT id, tags, geom FROM osm_way
WHERE tags ? 'natural'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'natural')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE tags ? 'natural';

-- Tourism carries no exclusion: nothing in the style draws a tourism way and its relation as one
-- area, so there is nothing to draw twice.

CREATE OR REPLACE VIEW osm_tourism AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'tourism'
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'tourism';

-- A building carries the height the style extrudes it by under any of several tags, in metres or
-- in levels, and it carries the zoning of the land it stands on, which is not on the building at
-- all. Deriving both here rather than in the style keeps that spelling out of every layer that
-- reads them. A building below ground is dropped: `layer` is negative and nothing above it would
-- be drawn over it.
--
-- The buildings are named apart from the view the layers read because two things need them: that
-- view, and the zoning join it reads, which has to see the same buildings without reading the view
-- its own result is joined into. A building is identified by which element it is as well as by its
-- id, way ids and relation ids being drawn from separate sequences that overlap.

CREATE OR REPLACE VIEW osm_building_element AS
SELECT 'way' AS element, id, tags, geom FROM osm_way
WHERE (tags ? 'building' OR tags ? 'building:part')
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id
                    AND tag_key IN ('building', 'building:part'))
  AND (NOT tags ? 'layer' OR convert_to_number(tags ->> 'layer', 0) >= 0)
UNION ALL
SELECT 'relation' AS element, id, tags, geom FROM osm_relation
WHERE (tags ? 'building' OR tags ? 'building:part')
  AND (NOT tags ? 'layer' OR convert_to_number(tags ->> 'layer', 0) >= 0);

-- The zoning a building stands on, resolved once rather than per tile.
--
-- OpenStreetMap has no zoning, so `landuse` stands in for it: it is what says that one part of a
-- town is industry and another is housing, and it is the only thing in the data that does. What a
-- point-in-polygon costs is the reason this is a table and not a join in the tile query: answering
-- it while a tile is built takes a dense tile from twenty milliseconds to several hundred, and
-- answering it once takes the same tile back to twenty.
--
-- Landuse polygons are subdivided first. A forest or a farm is one polygon of many thousand
-- vertices, and a bounding box test that hits it hands the whole outline to the point test; cut
-- into pieces small enough that the box means something, the index does the work it is there for.
-- The parts carry the area of the polygon they came from, because that is what ranks them: a
-- building standing on a retail park inside an industrial estate is on the retail park, and the
-- smaller area is the one that says so.

DROP MATERIALIZED VIEW IF EXISTS osm_zoning_part CASCADE;

CREATE MATERIALIZED VIEW osm_zoning_part AS
SELECT
    tags ->> 'landuse' AS zoning,
    ST_Area(geom) AS area,
    ST_Subdivide(geom, 128) AS geom
FROM osm_landuse
WHERE geom IS NOT NULL AND ST_Dimension(geom) = 2;

CREATE INDEX IF NOT EXISTS osm_zoning_part_geom_index ON osm_zoning_part USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_building_zoning CASCADE;

CREATE MATERIALIZED VIEW osm_building_zoning AS
SELECT DISTINCT ON (building.id, building.element)
    building.element,
    building.id,
    zone.zoning
FROM (
    SELECT element, id, ST_PointOnSurface(geom) AS geom
    FROM osm_building_element
    WHERE geom IS NOT NULL AND ST_Dimension(geom) = 2
) AS building
JOIN osm_zoning_part AS zone
  ON zone.geom && building.geom AND ST_Intersects(zone.geom, building.geom)
ORDER BY building.id, building.element, zone.area;

-- The zoning is carried in the index rather than only pointed at by it, so that the lookup a tile
-- makes for every building it draws is answered without reaching the table at all.

CREATE UNIQUE INDEX IF NOT EXISTS osm_building_zoning_index
    ON osm_building_zoning (id, element) INCLUDE (zoning);

CREATE OR REPLACE VIEW osm_building AS
SELECT
    building.id,
    building.tags || jsonb_build_object(
        'extrusion:base',
        CASE
            WHEN building.tags ? 'min_height'
                THEN convert_to_number(building.tags ->> 'min_height', 0)
            WHEN building.tags ? 'building:min_height'
                THEN convert_to_number(building.tags ->> 'building:min_height', 0)
            WHEN building.tags ? 'building:min_level'
                THEN convert_to_number(building.tags ->> 'building:min_level', 0) * 3
            ELSE 0
        END,
        'extrusion:height',
        CASE
            WHEN building.tags ? 'height'
                THEN convert_to_number(building.tags ->> 'height', 6)
            WHEN building.tags ? 'building:height'
                THEN convert_to_number(building.tags ->> 'building:height', 6)
            WHEN building.tags ? 'building:levels'
                THEN convert_to_number(building.tags ->> 'building:levels', 2) * 3
            ELSE 6
        END)
    -- A building on no zoning carries no zoning key, rather than one holding null: a key that is
    -- always present answers `tags ? 'zoning'` for every building and would quietly defeat the
    -- test a query uses to skip features it cannot draw.
    || CASE
           WHEN zoning.zoning IS NULL THEN '{}'::jsonb
           ELSE jsonb_build_object('zoning', zoning.zoning)
       END AS tags,
    building.geom
FROM osm_building_element AS building
LEFT JOIN osm_building_zoning AS zoning
       ON zoning.id = building.id AND zoning.element = building.element;
