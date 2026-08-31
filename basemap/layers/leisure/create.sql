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

-- Zoom levels 12 down to 4 generalize the level above them: nearby areas of the same value are
-- dilated until they touch, merged, eroded back by the same amount, and simplified. Dilating and
-- eroding by the same distance is a morphological closing, which keeps the result from drifting
-- outwards as the chain descends.
--
-- The area threshold is applied twice on purpose: once on the input, to keep small areas out of the
-- merge, and once on the result, to drop the slivers erosion leaves behind. Numbering the rows
-- after st_dump rather than alongside it is what gives each part its own id; a window function in
-- the same select list as a set-returning function is evaluated before the rows are expanded, so
-- every part of a cluster would otherwise share one id.
--
-- The intermediate results are common table expressions rather than materialized views: each one is
-- read exactly once, by the query that follows it.

-- Superseded by the common table expressions below.
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z12_filtered CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z11_filtered CASCADE;

DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z10_filtered CASCADE;

-- Zoom level 12
DROP
    MATERIALIZED VIEW IF EXISTS osm_leisure_z12 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z12 AS WITH filtered AS(
        SELECT
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
            )
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 12 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 12 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 12 ), 2 )* 32;

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
    MATERIALIZED VIEW IF EXISTS osm_leisure_z11 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z11 AS WITH filtered AS(
        SELECT
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
            AND st_area(geom)> POWER( 78270 / POWER( 2, 11 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 11 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 11 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 11 ), 2 )* 32;

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
    MATERIALIZED VIEW IF EXISTS osm_leisure_z10 CASCADE;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_leisure_z10 AS WITH filtered AS(
        SELECT
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
            AND st_area(geom)> POWER( 78270 / POWER( 2, 10 ), 2 )* 32
    ),
    clustered AS(
        SELECT
            tag,
            geom,
            st_clusterdbscan(
                geom,
                0,
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 10 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 10 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 10 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 9 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 9 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 9 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 8 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 8 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 8 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 7 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 7 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 7 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 6 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 6 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 6 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 5 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 5 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 5 ), 2 )* 32;

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
                1
            ) OVER(
                PARTITION BY tag
            ) AS cluster
        FROM
            filtered
    ),
    merged AS(
        SELECT
            tag,
            st_simplifypreservetopology(
                (
                    st_dump(
                        st_buffer(
                            st_collect(geom),
                            - 78270 / POWER( 2, 4 )* 1.1,
                            'join=mitre'
                        )
                    )
                ).geom,
                78270 / POWER( 2, 4 )
            ) AS geom
        FROM
            clustered
        GROUP BY
            tag,
            cluster
    ) SELECT
        ROW_NUMBER() OVER() AS id,
        jsonb_build_object(
            'leisure',
            tag
        ) AS tags,
        geom
    FROM
        merged
    WHERE
        geom IS NOT NULL
        AND NOT ST_IsEmpty(geom)
        AND st_area(geom)> POWER( 78270 / POWER( 2, 4 ), 2 )* 32;

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

-- Zoom levels 3 down to 1 relax the area threshold from 32 to 16 tolerances squared. At these
-- scales 32 tolerances squared is larger than most countries, so the stricter threshold used above
-- would leave these levels empty on anything short of a planet import.

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
        osm_leisure_z3
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
        osm_leisure_z2
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
