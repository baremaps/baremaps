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
-- Converts a string to a number
CREATE
    OR REPLACE FUNCTION convert_to_number(
        input_string text,
        default_value NUMERIC
    ) RETURNS NUMERIC AS $$ DECLARE RESULT NUMERIC;

BEGIN -- Replace comma with dot

input_string := REPLACE(
    input_string,
    ',',
    '.'
);

-- Use a regular expression to extract the first number from the string

input_string := SUBSTRING( input_string FROM '^[0-9]+\.?[0-9]*' );

-- Convert the extracted string to a numeric type
RESULT := input_string::NUMERIC;

IF RESULT IS NULL THEN RETURN default_value;
END IF;

RETURN RESULT;

EXCEPTION
WHEN OTHERS THEN -- Return the default value in case of any error
RETURN default_value;
END;

$$ LANGUAGE plpgsql;

-- How much of the map a named place is worth.
--
-- A place is admitted to a zoom by this number and, where two labels want the same piece of the
-- screen, MapLibre keeps the one that carries more of it. Both readings are the same number, held
-- as one attribute on the point, so the zoom a place first appears at and the label it beats
-- cannot disagree: they are the same statement made twice.
--
-- It is a count of people, so that it can be read. The class of the place says how many a place of
-- that kind stands for and the tag says how many it actually holds, and the larger of the two
-- wins: a class is a floor and not a default, so a town tagged with thirty inhabitants is still
-- worth a town, and a village of sixteen thousand is worth what it holds. Two thirds of the
-- villages in an extract carry a population and almost none of the hamlets and localities do,
-- which is why the floor cannot be left out: without it the places that need the ranking most are
-- the ones that would tie at zero.
--
-- A capital carries the traffic of the country or the region it administers rather than its own,
-- and a place with an article written about it is one somebody thought worth writing about. Both
-- are multipliers, so they lift a place within its own order of magnitude without letting a hamlet
-- out-rank a city.
--
-- The population is read here rather than through `convert_to_number`, which reads the same tag
-- the same way. This is called from the definition of a materialized view, and from PostgreSQL 18
-- a materialized view is built with a search path of `pg_catalog, pg_temp`: a function this one
-- calls by an unqualified name cannot be found when the body is inlined there, and qualifying it
-- would fix the schema the whole import is read from, which nothing else here does.
CREATE
    OR REPLACE FUNCTION place_rank(
        tags jsonb
    ) RETURNS NUMERIC AS $$ SELECT
        greatest(
            COALESCE(
                SUBSTRING( REPLACE( tags ->> 'population', ',', '.' ) FROM '^[0-9]+\.?[0-9]*' )::NUMERIC,
                0
            ),
            CASE
                tags ->> 'place'
                WHEN 'country' THEN 10000000
                WHEN 'state' THEN 1000000
                WHEN 'region' THEN 1000000
                WHEN 'province' THEN 1000000
                WHEN 'district' THEN 200000
                WHEN 'county' THEN 200000
                WHEN 'municipality' THEN 50000
                WHEN 'city' THEN 50000
                WHEN 'town' THEN 5000
                WHEN 'suburb' THEN 5000
                WHEN 'quarter' THEN 2000
                WHEN 'neighbourhood' THEN 1000
                WHEN 'village' THEN 500
                WHEN 'hamlet' THEN 100
                WHEN 'isolated_dwelling' THEN 20
                WHEN 'locality' THEN 10
                ELSE 0
            END
        )* CASE
            WHEN tags ? 'capital' THEN 4
            ELSE 1
        END * CASE
            WHEN tags ? 'wikipedia'
            OR tags ? 'wikidata' THEN 1.5
            ELSE 1
        END $$ LANGUAGE SQL IMMUTABLE;
