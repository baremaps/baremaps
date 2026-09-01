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
-- levels is named after it too, so the name is the handle rather than an alias. They read the base
-- tables and never each other, so the order below is alphabetical rather than a dependency.
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
-- in levels. Deriving it here rather than in the style keeps that spelling out of every layer that
-- reads it. A building below ground is dropped: `layer` is negative and nothing above it would be
-- drawn over it.

CREATE OR REPLACE VIEW osm_building AS
SELECT
    id,
    tags || jsonb_build_object(
        'extrusion:base',
        CASE
            WHEN tags ? 'min_height' THEN convert_to_number(tags ->> 'min_height', 0)
            WHEN tags ? 'building:min_height' THEN convert_to_number(tags ->> 'building:min_height', 0)
            WHEN tags ? 'building:min_level' THEN convert_to_number(tags ->> 'building:min_level', 0) * 3
            ELSE 0
        END,
        'extrusion:height',
        CASE
            WHEN tags ? 'height' THEN convert_to_number(tags ->> 'height', 6)
            WHEN tags ? 'building:height' THEN convert_to_number(tags ->> 'building:height', 6)
            WHEN tags ? 'building:levels' THEN convert_to_number(tags ->> 'building:levels', 2) * 3
            ELSE 6
        END) AS tags,
    geom
FROM osm_way
WHERE (tags ? 'building' OR tags ? 'building:part')
  AND NOT EXISTS (SELECT 1 FROM osm_member_tag
                  WHERE member_ref = osm_way.id
                    AND tag_key IN ('building', 'building:part'))
  AND (NOT tags ? 'layer' OR convert_to_number(tags ->> 'layer', 0) >= 0)
UNION
SELECT
    id,
    tags || jsonb_build_object(
        'extrusion:base',
        CASE
            WHEN tags ? 'min_height' THEN convert_to_number(tags ->> 'min_height', 0)
            WHEN tags ? 'building:min_height' THEN convert_to_number(tags ->> 'building:min_height', 0)
            WHEN tags ? 'building:min_level' THEN convert_to_number(tags ->> 'building:min_level', 0) * 3
            ELSE 0
        END,
        'extrusion:height',
        CASE
            WHEN tags ? 'height' THEN convert_to_number(tags ->> 'height', 6)
            WHEN tags ? 'building:height' THEN convert_to_number(tags ->> 'building:height', 6)
            WHEN tags ? 'building:levels' THEN convert_to_number(tags ->> 'building:levels', 2) * 3
            ELSE 6
        END) AS tags,
    geom
FROM osm_relation
WHERE (tags ? 'building' OR tags ? 'building:part')
  AND (NOT tags ? 'layer' OR convert_to_number(tags ->> 'layer', 0) >= 0);
