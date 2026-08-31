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
    OR REPLACE VIEW osm_route AS SELECT
        id,
        tags,
        geom
    FROM
        osm_linestring
    WHERE
        tags ? 'route';

CREATE
    OR REPLACE VIEW osm_route_z20 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z19 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z18 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z17 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z16 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z15 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z14 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

CREATE
    OR REPLACE VIEW osm_route_z13 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_route;

------------------
-- Features are dropped below a length of twice the simplification tolerance. The test used to
-- compare the area of the envelope against the square of the tolerance, which is orientation
-- dependent: the envelope of an axis-aligned line has zero area however long the line is, and among
-- roads of equal length the area spans a factor of thirty. Twice the tolerance reproduces the
-- density that filter happened to select, without depending on which way the line runs.

-- The filtered and clustered stages are common table expressions: each was read exactly once, by
-- the stage that followed it. Only the simplified result is materialized, because every zoom level
-- below reads it.
DROP
    MATERIALIZED VIEW IF EXISTS osm_route_filtered CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_clustered CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_simplified CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_route_simplified AS WITH filtered AS(
        SELECT
            tags -> 'route' AS route,
            geom AS geom
        FROM
            osm_route
        WHERE
            tags ->> 'route' IN(
                'road',
                'bus',
                'trolleybus',
                'route',
                'ferry',
                'train',
                'subway',
                'light_rail',
                'railway',
                'tram',
                'funicular'
            )
    ),
    clustered AS(
        SELECT
            route,
            geom,
            ST_ClusterDBSCAN(
                geom,
                0,
                1
            ) OVER(
                PARTITION BY route
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            route AS route,
            ST_LineMerge(
                ST_Collect(geom)
            ) AS geom
        FROM
            clustered
        GROUP BY
            route,
            cluster
    ),
    exploded AS(
        SELECT
            route AS route,
            (
                ST_Dump(geom)
            ).geom AS geom
        FROM
            merged
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'route',
            route
        ) AS tags,
        geom AS geom
    FROM
        exploded;

DROP
    INDEX IF EXISTS osm_route_simplified_geom;

CREATE
    INDEX IF NOT EXISTS osm_route_simplified_geom ON
    osm_route_simplified
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z12 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_route_z12 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 12 )
        ) AS geom
    FROM
        osm_route_simplified
    WHERE
        geom IS NOT NULL
        AND st_length(geom)> 78270 / POWER( 2, 12 )* 2;

DROP
    INDEX IF EXISTS osm_route_z12_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_route_z12_geom_idx ON
    osm_route_z12
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z11 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_route_z11 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 11 )
        ) AS geom
    FROM
        osm_route_simplified
    WHERE
        geom IS NOT NULL
        AND st_length(geom)> 78270 / POWER( 2, 11 )* 2;

DROP
    INDEX IF EXISTS osm_route_z11_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_route_z11_geom_idx ON
    osm_route_z11
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z10 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_route_z10 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 10 )
        ) AS geom
    FROM
        osm_route_simplified
    WHERE
        geom IS NOT NULL
        AND st_length(geom)> 78270 / POWER( 2, 10 )* 2;

DROP
    INDEX IF EXISTS osm_route_z10_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_route_z10_geom_idx ON
    osm_route_z10
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z9 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_route_z9 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 9 )
        ) AS geom
    FROM
        osm_route_simplified
    WHERE
        geom IS NOT NULL
        AND st_length(geom)> 78270 / POWER( 2, 9 )* 2;

DROP
    INDEX IF EXISTS osm_route_z9_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_route_z9_geom_idx ON
    osm_route_z9
        USING GIST(geom);

-- Zoom levels below 9 are not queried: layers/route/tileset.js serves this layer from
-- zoom 9 up. The views are only dropped, so a database created before this change sheds
-- them instead of refreshing views nothing reads.
DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z8 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z7 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z6 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z5 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z4 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z3 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z2 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_route_z1 CASCADE;
