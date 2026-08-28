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
    OR REPLACE VIEW osm_railway AS SELECT
        id,
        tags,
        geom
    FROM
        osm_way
    WHERE
        tags ? 'railway';

CREATE
    OR REPLACE VIEW osm_railway_z20 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z19 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z18 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z17 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z16 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z15 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z14 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

CREATE
    OR REPLACE VIEW osm_railway_z13 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_railway;

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_filtered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_filtered AS SELECT
        tags -> 'railway' AS railway,
        geom AS geom
    FROM
        osm_railway
    WHERE
        tags ->> 'railway' IN(
            'light_rail',
            'monorail',
            'rail',
            'subway',
            'tram'
        )
        AND NOT tags ? 'service';

DROP
    INDEX IF EXISTS osm_railway_filtered_geom;

CREATE
    INDEX IF NOT EXISTS osm_railway_filtered_geom ON
    osm_railway_filtered
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_clustered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_clustered AS SELECT
        railway AS railway,
        geom AS geom,
        ST_ClusterDBSCAN(
            geom,
            0,
            1
        ) OVER(
            PARTITION BY railway
        ) AS cluster
    FROM
        osm_railway_filtered;

DROP
    INDEX IF EXISTS osm_railway_clustered_geom;

CREATE
    INDEX IF NOT EXISTS osm_railway_clustered_geom ON
    osm_railway_clustered
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_simplified CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_simplified AS WITH merged AS(
        SELECT
            railway AS railway,
            ST_LineMerge(
                ST_Collect(geom)
            ) AS geom
        FROM
            osm_railway_clustered
        GROUP BY
            railway,
            cluster
    ),
    exploded AS(
        SELECT
            railway AS railway,
            (
                ST_Dump(geom)
            ).geom AS geom
        FROM
            merged
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'railway',
            railway
        ) AS tags,
        geom AS geom
    FROM
        exploded;

DROP
    INDEX IF EXISTS osm_railway_simplified_geom;

CREATE
    INDEX IF NOT EXISTS osm_railway_simplified_geom ON
    osm_railway_simplified
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z12 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z12 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 12 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 12 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z12_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z12_geom_idx ON
    osm_railway_z12
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z11 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z11 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 11 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 11 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z11_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z11_geom_idx ON
    osm_railway_z11
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z10 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z10 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 10 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 10 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z10_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z10_geom_idx ON
    osm_railway_z10
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z9 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z9 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 9 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 9 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z9_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z9_geom_idx ON
    osm_railway_z9
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z8 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z8 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 8 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 8 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z8_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z8_geom_idx ON
    osm_railway_z8
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z7 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z7 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 7 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 7 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z7_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z7_geom_idx ON
    osm_railway_z7
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z6 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z6 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 6 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 6 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z6_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z6_geom_idx ON
    osm_railway_z6
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z5 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z5 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 5 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 5 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z5_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z5_geom_idx ON
    osm_railway_z5
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z4 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z4 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 4 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 4 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z4_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z4_geom_idx ON
    osm_railway_z4
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z3 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z3 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 3 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 3 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z3_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z3_geom_idx ON
    osm_railway_z3
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z2 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z2 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 2 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 2 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z2_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z2_geom_idx ON
    osm_railway_z2
        USING GIST(geom);

DROP
    MATERIALIZED VIEW IF EXISTS osm_railway_z1 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_railway_z1 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 1 )
        ) AS geom
    FROM
        osm_railway_simplified
    WHERE
        geom IS NOT NULL
        AND(
            st_area(
                st_envelope(geom)
            )> POWER(( 78270 / POWER( 2, 1 )), 2 )
        );

DROP
    INDEX IF EXISTS osm_railway_z1_geom_idx;

CREATE
    INDEX IF NOT EXISTS osm_railway_z1_geom_idx ON
    osm_railway_z1
        USING GIST(geom);
