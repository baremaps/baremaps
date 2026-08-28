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
    MATERIALIZED VIEW IF NOT EXISTS overture_admins_administrative_boundary_materialized_view AS SELECT -- Generate a unique id for each rowROW_NUMBER() OVER() AS id, -- Rename the geometry column
        st_simplifypreservetopology(
            geometry,
            78270 / POWER( 2, 2 )
        ) AS geom, -- Aggregate other fields into a jsonb tags field
        jsonb_build_object(
            'admin_level',
            admin_level,
            'version',
            version,
            'sources',
            sources,
            'population',
            population,
            'names',
            names
        ) AS tags
    FROM
        overture_admins_administrative_boundary;