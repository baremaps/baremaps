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
    OR REPLACE VIEW osm_point AS SELECT
        id,
        tags,
        geom
    FROM
        osm_node
    WHERE
        geom IS NOT NULL
        AND tags != '{}';

CREATE
    OR REPLACE VIEW osm_point_z20 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z19 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z18 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z17 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z16 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z15 AS SELECT
        *
    FROM
        osm_point;

CREATE
    OR REPLACE VIEW osm_point_z14 AS SELECT
        *
    FROM
        osm_point;

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z13;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z13 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town',
            'village',
            'quarter',
            'hamlet' ]
        )
        OR(
            tags ->> 'natural'
        )= ANY(
            ARRAY [ 'peak',
            'volcano',
            'spring' ]
        )
        OR(
            tags ->> 'highway'
        )= 'motorway_junction'
        OR(
            tags ->> 'tourism'
        )= 'wilderness_hut'
        OR(
            tags ->> 'waterway'
        )= 'waterfall'
        OR(
            tags ->> 'railway'
        )= 'level_crossing';

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z12;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z12 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town',
            'village' ]
        )
        OR(
            tags ->> 'natural'
        )= ANY(
            ARRAY [ 'peak',
            'volcano' ]
        )
        OR(
            tags ->> 'highway'
        )= 'motorway_junction'
        OR(
            tags ->> 'tourism'
        )= 'wilderness_hut'
        OR(
            tags ->> 'waterway'
        )= 'waterfall';

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z11;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z11 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'state',
            'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town',
            'village' ]
        )
        OR(
            tags ->> 'natural'
        )= ANY(
            ARRAY [ 'peak',
            'volcano' ]
        )
        OR(
            tags ->> 'highway'
        )= 'motorway_junction';

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z10;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z10 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'state',
            'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town' ]
        )
        OR(
            tags ->> 'natural'
        )= ANY(
            ARRAY [ 'peak',
            'volcano' ]
        )
        OR(
            tags ->> 'highway'
        )= 'motorway_junction';

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z9;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z9 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'state',
            'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z8;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z8 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'state',
            'region',
            'province',
            'district',
            'county',
            'municipality',
            'city',
            'town' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z7;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z7 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'city',
            'sea',
            'state',
            'county' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z6;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z6 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'city',
            'sea',
            'state',
            'county' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z5;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z5 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'city',
            'sea',
            'state',
            'county' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z4;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z4 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'city',
            'sea' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z3;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z3 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= ANY(
            ARRAY [ 'country',
            'city',
            'sea' ]
        );

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z2;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z2 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= 'country';

DROP
    MATERIALIZED VIEW IF EXISTS osm_point_z1;

CREATE
    MATERIALIZED VIEW IF NOT EXISTS osm_point_z1 AS SELECT
        id,
        tags,
        geom
    FROM
        osm_point
    WHERE
        (
            tags ->> 'place'
        )= 'country';

DROP
    INDEX IF EXISTS osm_point_geom_z13_index;

DROP
    INDEX IF EXISTS osm_point_geom_z12_index;

DROP
    INDEX IF EXISTS osm_point_geom_z11_index;

DROP
    INDEX IF EXISTS osm_point_geom_z10_index;

DROP
    INDEX IF EXISTS osm_point_geom_z9_index;

DROP
    INDEX IF EXISTS osm_point_geom_z8_index;

DROP
    INDEX IF EXISTS osm_point_geom_z7_index;

DROP
    INDEX IF EXISTS osm_point_geom_z6_index;

DROP
    INDEX IF EXISTS osm_point_geom_z5_index;

DROP
    INDEX IF EXISTS osm_point_geom_z4_index;

DROP
    INDEX IF EXISTS osm_point_geom_z3_index;

DROP
    INDEX IF EXISTS osm_point_geom_z2_index;

DROP
    INDEX IF EXISTS osm_point_geom_z1_index;

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z13_index ON
    osm_point_z13
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z12_index ON
    osm_point_z12
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z11_index ON
    osm_point_z11
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z10_index ON
    osm_point_z10
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z9_index ON
    osm_point_z9
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z8_index ON
    osm_point_z8
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z7_index ON
    osm_point_z7
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z6_index ON
    osm_point_z6
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z5_index ON
    osm_point_z5
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z4_index ON
    osm_point_z4
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z3_index ON
    osm_point_z3
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z2_index ON
    osm_point_z2
        USING gist(geom);

CREATE
    INDEX IF NOT EXISTS osm_point_geom_z1_index ON
    osm_point_z1
        USING gist(geom);
