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
-- Zoom levels 20 to 13
CREATE
    OR REPLACE VIEW osm_leisure AS SELECT
        id,
        tags,
        geom
    FROM
        osm_way
    WHERE
        tags ? 'leisure'
        AND NOT EXISTS(
            SELECT
                1
            FROM
                osm_member_tag
            WHERE
                member_ref = osm_way.id
                AND tag_key = 'leisure'
        )
UNION SELECT
        id,
        tags,
        geom
    FROM
        osm_relation
    WHERE
        tags ? 'leisure';

CREATE
    OR REPLACE VIEW osm_leisure_z20 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z19 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z18 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z17 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z16 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z15 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z14 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

CREATE
    OR REPLACE VIEW osm_leisure_z13 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_leisure;

-- Zoom level 12
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z12_filtered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z12_filtered AS SELECT
        tags -> 'leisure' AS tag,
        st_buffer(
            st_simplifypreservetopology(
                geom,
                78270 / POWER( 2, 12 )
            ),
            78270 / POWER( 2, 12 )* 1.1,
            'join=mitre'
        ) AS geom
    FROM
        osm_leisure
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 12 ), 2 )* 32
        AND tags ->> 'leisure' IN(
            'garden',
            'golf_course',
            'marina',
            'nature_reserve',
            'park',
            'pitch',
            'sport_center',
            'stadium',
            'swimming_pool',
            'track'
        );

-- No geometry index on this intermediate: its only reader scans it in full, and
-- ST_ClusterDBSCAN builds its own index rather than using one.
DROP
    INDEX IF EXISTS osm_leisure_z12_filtered_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z12_filtered_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z12_filtered_tags_idx ON
    osm_leisure_z12_filtered(tag);

DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z12 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z12 AS WITH clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            osm_leisure_z12_filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 12 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 12 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 12 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z12_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z12_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z12_geom_idx ON
    osm_leisure_z12
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z12_tags_idx ON
    osm_leisure_z12
        USING GIN(tags);

-- Zoom level 11
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z11_filtered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z11_filtered AS SELECT
        tags -> 'leisure' AS tag,
        st_buffer(
            st_simplifypreservetopology(
                geom,
                78270 / POWER( 2, 11 )
            ),
            78270 / POWER( 2, 11 )* 1.1,
            'join=mitre'
        ) AS geom
    FROM
        osm_leisure_z12
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 11 ), 2 )* 32;

-- No geometry index on this intermediate: its only reader scans it in full, and
-- ST_ClusterDBSCAN builds its own index rather than using one.
DROP
    INDEX IF EXISTS osm_leisure_z11_filtered_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z11_filtered_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z11_filtered_tags_idx ON
    osm_leisure_z11_filtered(tag);

DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z11 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z11 AS WITH clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            osm_leisure_z11_filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 11 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 11 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 11 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z11_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z11_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z11_geom_idx ON
    osm_leisure_z11
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z11_tags_idx ON
    osm_leisure_z11
        USING GIN(tags);

-- Zoom level 10
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z10_filtered CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z10_filtered AS SELECT
        tags -> 'leisure' AS tag,
        st_buffer(
            st_simplifypreservetopology(
                geom,
                78270 / POWER( 2, 10 )
            ),
            78270 / POWER( 2, 10 )* 1.1,
            'join=mitre'
        ) AS geom
    FROM
        osm_leisure_z11
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 10 ), 2 )* 32;

-- No geometry index on this intermediate: its only reader scans it in full, and
-- ST_ClusterDBSCAN builds its own index rather than using one.
DROP
    INDEX IF EXISTS osm_leisure_z10_filtered_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z10_filtered_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z10_filtered_tags_idx ON
    osm_leisure_z10_filtered(tag);

DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z10 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z10 AS WITH clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            osm_leisure_z10_filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 10 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 10 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 10 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z10_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z10_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z10_geom_idx ON
    osm_leisure_z10
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z10_tags_idx ON
    osm_leisure_z10
        USING GIN(tags);

-- Zoom level 9
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z9 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z9 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 9 )
                ),
                78270 / POWER( 2, 9 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z10
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 9 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 9 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 9 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 9 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z9_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z9_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z9_geom_idx ON
    osm_leisure_z9
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z9_tags_idx ON
    osm_leisure_z9
        USING GIN(tags);

-- Zoom level 8
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z8 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z8 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 8 )
                ),
                78270 / POWER( 2, 8 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z9
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 8 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 8 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 8 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 8 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z8_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z8_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z8_geom_idx ON
    osm_leisure_z8
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z8_tags_idx ON
    osm_leisure_z8
        USING GIN(tags);

-- Zoom level 7
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z7 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z7 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 7 )
                ),
                78270 / POWER( 2, 7 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z8
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 7 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 7 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 7 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 7 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z7_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z7_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z7_geom_idx ON
    osm_leisure_z7
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z7_tags_idx ON
    osm_leisure_z7
        USING GIN(tags);

-- Zoom level 6
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z6 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z6 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 6 )
                ),
                78270 / POWER( 2, 6 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z7
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 6 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 6 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 6 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 6 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z6_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z6_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z6_geom_idx ON
    osm_leisure_z6
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z6_tags_idx ON
    osm_leisure_z6
        USING GIN(tags);

-- Zoom level 5
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z5 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z5 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 5 )
                ),
                78270 / POWER( 2, 5 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z6
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 5 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 5 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 5 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 5 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z5_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z5_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z5_geom_idx ON
    osm_leisure_z5
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z5_tags_idx ON
    osm_leisure_z5
        USING GIN(tags);

-- Zoom level 4
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z4 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z4 AS WITH filtered AS(
        SELECT
            tags -> 'leisure' AS tag,
            st_buffer(
                st_simplifypreservetopology(
                    geom,
                    78270 / POWER( 2, 4 )
                ),
                78270 / POWER( 2, 4 )* 1.1,
                'join=mitre'
            ) AS geom
        FROM
            osm_leisure_z5
        WHERE
            geom IS NOT NULL
            AND NOT ST_IsEmpty(geom)
            AND st_area(geom)> POWER( 78270 / POWER( 2, 4 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                0
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        st_simplifypreservetopology(
            (
                st_dump(
                    st_buffer(
                        st_collect(geom),
                        - 78270 / POWER( 2, 4 ),
                        'join=mitre'
                    )
                )
            ).geom,
            78270 / POWER( 2, 4 )
        ) AS geom
    FROM
        clustered
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 4 ), 2 )* 32
    GROUP BY
        tag,
        cluster;

DROP
    INDEX IF EXISTS osm_leisure_z4_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z4_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z4_geom_idx ON
    osm_leisure_z4
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z4_tags_idx ON
    osm_leisure_z4
        USING GIN(tags);

-- Zoom level 3
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z3 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z3 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 3 )
        ) AS geom
    FROM
        osm_leisure_z4
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 3 ), 2 )* 16;

DROP
    INDEX IF EXISTS osm_leisure_z3_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z3_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z3_geom_idx ON
    osm_leisure_z3
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z3_tags_idx ON
    osm_leisure_z3
        USING GIN(tags);

-- Zoom level 2
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z2 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z2 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 2 )
        ) AS geom
    FROM
        osm_leisure_z4
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 2 ), 2 )* 16;

DROP
    INDEX IF EXISTS osm_leisure_z2_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z2_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z2_geom_idx ON
    osm_leisure_z2
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z2_tags_idx ON
    osm_leisure_z2
        USING GIN(tags);

-- Zoom level 1
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z1 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z1 AS SELECT
        id,
        tags,
        st_simplifypreservetopology(
            geom,
            78270 / POWER( 2, 1 )
        ) AS geom
    FROM
        osm_leisure_z4
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 1 ), 2 )* 16;

DROP
    INDEX IF EXISTS osm_leisure_z1_geom_idx;

DROP
    INDEX IF EXISTS osm_leisure_z1_tags_idx;

CREATE
    INDEX IF NOT EXISTS osm_leisure_z1_geom_idx ON
    osm_leisure_z1
        USING GIST(geom);

CREATE
    INDEX IF NOT EXISTS osm_leisure_z1_tags_idx ON
    osm_leisure_z1
        USING GIN(tags);
