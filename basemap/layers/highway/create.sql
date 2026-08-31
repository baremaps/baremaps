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
-- Zoom levels 20-13
CREATE
    OR REPLACE VIEW osm_highway AS SELECT
        id,
        tags,
        geom
    FROM
        osm_way
    WHERE
        osm_way.geom IS NOT NULL
        AND tags ? 'highway';

CREATE
    OR REPLACE VIEW osm_highway_z20 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z19 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z18 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z17 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z16 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z15 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z14 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway;

CREATE
    OR REPLACE VIEW osm_highway_z13 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_highway
    WHERE
        tags ->> 'highway' IN(
            'motorway',
            'motorway_link',
            'trunk',
            'trunk_link',
            'primary',
            'primary_link',
            'secondary',
            'secondary_link',
            'tertiary',
            'tertiary_link',
            'unclassified',
            'residential'
        );

-- Features are dropped below a length of twice the simplification tolerance. The test used to
-- compare the area of the envelope against the square of the tolerance, which is orientation
-- dependent: the envelope of an axis-aligned line has zero area however long the line is, and among
-- roads of equal length the area spans a factor of thirty. Twice the tolerance reproduces the
-- density that filter happened to select, without depending on which way the line runs.
DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_filtered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_filtered AS SELECT
        tags -> 'highway' AS highway,
        geom AS geom
    FROM
        osm_highway
    WHERE
        tags ->> 'highway' IN(
            'motorway',
            'motorway_link',
            'trunk',
            'trunk_link',
            'primary',
            'primary_link',
            'secondary',
            'secondary_link',
            'tertiary',
            'tertiary_link',
            'unclassified',
            'residential'
        );

-- No geometry index on this intermediate: its only reader scans it in full, and
-- ST_ClusterDBSCAN builds its own index rather than using one.
DROP
    INDEX IF EXISTS osm_highway_filtered_geom;

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_clustered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_clustered AS SELECT
        highway AS highway,
        geom AS geom,
        ST_ClusterDBSCAN(
            geom,
            0,
            1
        ) OVER(
            PARTITION BY highway
        ) AS cluster
    FROM
        osm_highway_filtered;

-- No geometry index on this intermediate: its only reader scans it in full, and
-- ST_ClusterDBSCAN builds its own index rather than using one.
DROP
    INDEX IF EXISTS osm_highway_clustered_geom;

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_simplified CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_simplified AS WITH merged AS(
        SELECT
            highway AS highway,
            ST_LineMerge(
                ST_Collect(geom)
            ) AS geom
        FROM
            osm_highway_clustered
        GROUP BY
            highway,
            cluster
    ),
    exploded AS(
        SELECT
            highway AS highway,
            (
                ST_Dump(geom)
            ).geom AS geom
        FROM
            merged
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'highway',
            highway
        ) AS tags,
        geom AS geom
    FROM
        exploded;

DROP
    INDEX IF EXISTS osm_highway_simplified_geom;

CREATE
    INDEX IF NOT EXISTS osm_highway_simplified_geom ON
    osm_highway_simplified
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z12 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z12 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 12 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 12 )* 2;

DROP
    INDEX IF EXISTS osm_highway_z12_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z12_geom_idx ON
    osm_highway_z12
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z11 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z11 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 11 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 11 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'motorway_link',
            'trunk',
            'trunk_link',
            'primary',
            'primary_link',
            'secondary',
            'secondary_link',
            'tertiary',
            'tertiary_link',
            'unclassified',
            'residential'
        );

DROP
    INDEX IF EXISTS osm_highway_z11_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z11_geom_idx ON
    osm_highway_z11
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z10 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z10 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 10 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 10 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'motorway_link',
            'trunk',
            'trunk_link',
            'primary',
            'primary_link',
            'secondary',
            'secondary_link',
            'tertiary',
            'tertiary_link'
        );

DROP
    INDEX IF EXISTS osm_highway_z10_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z10_geom_idx ON
    osm_highway_z10
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z9 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z9 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 9 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 9 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'motorway_link',
            'trunk',
            'trunk_link',
            'primary',
            'primary_link',
            'secondary',
            'secondary_link'
        );

DROP
    INDEX IF EXISTS osm_highway_z9_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z9_geom_idx ON
    osm_highway_z9
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z8 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z8 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 8 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 8 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'trunk',
            'primary'
        );

DROP
    INDEX IF EXISTS osm_highway_z8_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z8_geom_idx ON
    osm_highway_z8
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z7 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z7 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 7 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 7 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'trunk',
            'primary'
        );

DROP
    INDEX IF EXISTS osm_highway_z7_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z7_geom_idx ON
    osm_highway_z7
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z6 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z6 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 6 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 6 )* 2
        AND tags ->> 'highway' IN(
            'motorway',
            'trunk',
            'primary'
        );

DROP
    INDEX IF EXISTS osm_highway_z6_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z6_geom_idx ON
    osm_highway_z6
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z5 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z5 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 5 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 5 )* 2
        AND tags ->> 'highway' IN('motorway');

DROP
    INDEX IF EXISTS osm_highway_z5_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z5_geom_idx ON
    osm_highway_z5
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z4 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_highway_z4 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 4 )
        ) AS geom
    FROM
        osm_highway_simplified
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_length(geom)> 78270 / POWER( 2, 4 )* 2
        AND tags ->> 'highway' IN('motorway');

DROP
    INDEX IF EXISTS osm_highway_z4_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_highway_z4_geom_idx ON
    osm_highway_z4
        USING GIST(geom);

-- Zoom levels below 4 are not queried: layers/highway/tileset.js serves this layer from
-- zoom 4 up. The views are only dropped, so a database created before this change sheds
-- them instead of refreshing views nothing reads.
DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z3 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z2 CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_highway_z1 CASCADE;
