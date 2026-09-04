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

-- One relation per source layer: where each layer's features come from.
--
-- A layer names one of these in its `sourceQueries`, and a generalized layer's chain of zoom
-- levels is named after it too, so the name is the handle rather than an alias. Most read the base
-- tables and nothing else, so the order below is alphabetical rather than a dependency. The
-- buildings at the end are the exception: a building is coloured by the land it stands on, so it
-- reads the landuse, and the two are therefore written in the order they have to be created in.
--
-- Most are one filter on osm_way, and are kept side by side rather than each in its own file so
-- that a subject styled differently from its neighbours is visible instead of buried.
--
-- They are stored rather than left as queries, and each is written out in the order its geometry
-- index reads it. Both were measured on a dense tile at zoom 14 over Zurich, and both come from
-- the same fact: a tile asks for a few thousand features out of millions, and what it pays is the
-- number of 8 kB pages those features are spread across.
--
-- The order is the larger half. osm_way is written in the order the ids arrive, which is the order
-- the ways were drawn over twenty years rather than where they are, so 12,550 ways inside that
-- tile sat on 3,008 distinct pages -- four rows to a page, the rest of each page belonging to some
-- other part of the world. Written out in the order the index visits them, neighbours share a
-- page: the same buildings came back in 1,243 pages instead of 3,093, and 15 ms instead of 143.
-- `ORDER BY geom` is what asks for it, PostGIS sorting geometries along a Hilbert curve, and a
-- refresh re-runs this query and so keeps the order.
--
-- Storing is the other half, and it is what takes the work that does not depend on the tile out of
-- the tile. A view is substituted into every tile query that names it, so the anti-join below ran
-- once per tile against every multipolygon member in the extract, and the zoning was looked up per
-- building drawn; between them the building layer touched 14,066 pages to draw 3,579 buildings.
-- Stored, it touches 529, and takes 24 ms rather than 182.
--
-- What that costs is staleness and about 1.7 GB on a Swiss extract. An update writes to the base
-- tables and does not reach these, so `refresh.js` rebuilds them; it refreshes in dependency
-- order, so a relation here may read another.

DROP MATERIALIZED VIEW IF EXISTS osm_aerialway CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_aerialway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'aerialway'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_aerialway_geom_index ON osm_aerialway USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_aeroway CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_aeroway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'aeroway'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_aeroway_geom_index ON osm_aeroway USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_attraction CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_attraction AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'attraction'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_attraction_geom_index ON osm_attraction USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_barrier CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_barrier AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'barrier'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_barrier_geom_index ON osm_barrier USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_boundary CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_boundary AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'boundary'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_boundary_geom_index ON osm_boundary USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_highway CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_highway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'highway'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_highway_geom_index ON osm_highway USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_man_made CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_man_made AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'man_made'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_man_made_geom_index ON osm_man_made USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_power CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_power AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL
  AND tags ->> 'power' IN ('cable', 'line', 'minor_line', 'plant', 'substation')
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_power_geom_index ON osm_power USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_railway CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_railway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'railway'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_railway_geom_index ON osm_railway USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_waterway CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_waterway AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'waterway'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_waterway_geom_index ON osm_waterway USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_route CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_route AS
SELECT id, tags, geom FROM osm_linestring
WHERE geom IS NOT NULL AND tags ? 'route'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_route_geom_index ON osm_route USING GIST(geom);

-- An area can be tagged on the way that outlines it or on the multipolygon that collects those
-- ways, so these read both. A way whose relation already carries the tag is dropped, because the
-- relation is the same area and drawing both draws it twice.

DROP MATERIALIZED VIEW IF EXISTS osm_amenity CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_amenity AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'amenity'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'amenity')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'amenity'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_amenity_geom_index ON osm_amenity USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_landuse CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_landuse AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'landuse'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'landuse')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'landuse'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_landuse_geom_index ON osm_landuse USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_leisure CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_leisure AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'leisure'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'leisure')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'leisure'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_leisure_geom_index ON osm_leisure USING GIST(geom);

DROP MATERIALIZED VIEW IF EXISTS osm_natural CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_natural AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'natural'
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id AND tag_key = 'natural')
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'natural'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_natural_geom_index ON osm_natural USING GIST(geom);

-- Tourism carries no exclusion: nothing in the style draws a tourism way and its relation as one
-- area, so there is nothing to draw twice.

DROP MATERIALIZED VIEW IF EXISTS osm_tourism CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_tourism AS
SELECT id, tags, geom FROM osm_way
WHERE geom IS NOT NULL AND tags ? 'tourism'
UNION
SELECT id, tags, geom FROM osm_relation
WHERE geom IS NOT NULL AND tags ? 'tourism'
ORDER BY geom;

CREATE INDEX IF NOT EXISTS osm_tourism_geom_index ON osm_tourism USING GIST(geom);

-- A building carries the height the style extrudes it by under any of several tags, in metres or
-- in levels, and it carries the zoning of the land it stands on, which is not on the building at
-- all. Deriving both here rather than in the style keeps that spelling out of every layer that
-- reads them. A building below ground is dropped: `layer` is negative and nothing above it would
-- be drawn over it.
--
-- The buildings are named apart from the relation the layers read because two things need them:
-- that relation, and the zoning join it reads, which has to see the same buildings without reading
-- the relation its own result is joined into. A building is identified by which element it is as
-- well as by its id, way ids and relation ids being drawn from separate sequences that overlap.

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

-- The zoning a building stands on.
--
-- OpenStreetMap has no zoning, so `landuse` stands in for it: it is what says that one part of a
-- town is industry and another is housing, and it is the only thing in the data that does. What a
-- point-in-polygon costs is why this is resolved when the buildings are built rather than while a
-- tile is: answering it per tile takes a dense tile from twenty milliseconds to several hundred.
--
-- Landuse polygons are subdivided first. A forest or a farm is one polygon of many thousand
-- vertices, and a bounding box test that hits it hands the whole outline to the point test; cut
-- into pieces small enough that the box means something, the index does the work it is there for.
-- The parts carry the area of the polygon they came from, because that is what ranks them: a
-- building standing on a retail park inside an industrial estate is on the retail park, and the
-- smaller area is the one that says so.

DROP MATERIALIZED VIEW IF EXISTS osm_zoning_part CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_zoning_part AS
SELECT
    tags ->> 'landuse' AS zoning,
    ST_Area(geom) AS area,
    ST_Subdivide(geom, 128) AS geom
FROM osm_landuse
WHERE geom IS NOT NULL AND ST_Dimension(geom) = 2;

CREATE INDEX IF NOT EXISTS osm_zoning_part_geom_index ON osm_zoning_part USING GIST(geom);

-- The zoning is read once, by the buildings below, so it is worked out where it is read rather
-- than stored a second time under a name of its own.

CREATE OR REPLACE VIEW osm_building_zoning AS
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

DROP MATERIALIZED VIEW IF EXISTS osm_building CASCADE;

CREATE MATERIALIZED VIEW IF NOT EXISTS osm_building AS
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
       ON zoning.id = building.id AND zoning.element = building.element
WHERE building.geom IS NOT NULL
ORDER BY building.geom;

CREATE INDEX IF NOT EXISTS osm_building_geom_index ON osm_building USING GIST(geom);
