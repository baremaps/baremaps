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
    MATERIALIZED VIEW IF EXISTS osm_member CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_member AS SELECT
        DISTINCT member_ref AS member_ref
    FROM
        osm_relation,
        UNNEST(
            member_types,
            member_refs
        ) AS way(
            member_type,
            member_ref
        )
    WHERE
        geom IS NOT NULL
        AND member_type = 1
        AND tags ->> 'type' = 'multipolygon' -- COALESCE, not NOT (... = ...): tags ->> 'natural' is NULL for the vast majority of
-- multipolygons, and NOT (NULL = 'coastline') is NULL, which drops the row.

        AND COALESCE(
            tags ->> 'natural',
            ''
        )<> 'coastline';

DROP
    INDEX IF EXISTS osm_member_idx;

CREATE
    INDEX IF NOT EXISTS osm_member_idx ON
    osm_member(member_ref);

-- The tag keys a way's parent multipolygons already carry.
--
-- A way that closes a multipolygon ring is drawn a second time by any thematic layer that also
-- selects it directly, but only when the parent relation carries the same key: a way tagged
-- natural=wood inside a landuse=residential multipolygon is not drawn by the landuse relation and
-- has to stay. Keys are restricted to the ones the thematic layers select on, which keeps the view
-- small enough to be worth materializing.
DROP
    MATERIALIZED VIEW IF EXISTS osm_member_tag CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_member_tag AS SELECT
        DISTINCT member_ref AS member_ref,
        tag_key AS tag_key
    FROM
        osm_relation,
        UNNEST(
            member_types,
            member_refs
        ) AS way(
            member_type,
            member_ref
        ),
        LATERAL UNNEST(
            ARRAY [ 'natural',
            'landuse',
            'leisure',
            'amenity',
            'building',
            'building:part' ]
        ) AS tag_key
    WHERE
        geom IS NOT NULL
        AND member_type = 1
        AND tags ->> 'type' = 'multipolygon'
        AND COALESCE(
            tags ->> 'natural',
            ''
        )<> 'coastline'
        AND tags ? tag_key;

DROP
    INDEX IF EXISTS osm_member_tag_idx;

CREATE
    INDEX IF NOT EXISTS osm_member_tag_idx ON
    osm_member_tag(
        tag_key,
        member_ref
    );
