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
-- Asserts that the test function raises the expected exception.
CREATE
    OR REPLACE FUNCTION assert_exception(
        test_query text,
        expected_exception text
    ) RETURNS void AS $$ BEGIN BEGIN -- Execute the test query using dynamic SQL
EXECUTE test_query;

-- If no exception is raised, fail the test

RAISE EXCEPTION 'Assertion Failed: Expected exception %, but no exception was raised',
expected_exception;

EXCEPTION
WHEN OTHERS THEN -- Check if the raised exception matches the expected exception
IF SQLERRM NOT LIKE expected_exception THEN RAISE EXCEPTION 'Assertion Failed: Expected exception %, but was %',
expected_exception,
SQLERRM;
END IF;
END;
END;

$$ LANGUAGE plpgsql;

-- Asserts that the actual value is equal to the expected value.
CREATE
    OR REPLACE FUNCTION assert_equals(
        actual NUMERIC,
        expected NUMERIC
    ) RETURNS void AS $$ BEGIN IF actual != expected THEN RAISE EXCEPTION 'Assertion Failed: Expected %, but was %',
    expected,
    actual;
END IF;
END;

$$ LANGUAGE plpgsql;

-- Test cases for the assert_equals function
SELECT
    assert_equals(
        1,
        1
    );

SELECT
    assert_exception(
        'SELECT assert_equals(1, 2)',
        'Assertion Failed: Expected 2, but was 1'
    );

-- Test cases for the convert_to_number function
SELECT
    assert_equals(
        convert_to_number(
            '1',
            0
        ),
        1
    );

SELECT
    assert_equals(
        convert_to_number(
            '2.3',
            0
        ),
        2.3
    );

SELECT
    assert_equals(
        convert_to_number(
            '3,4',
            0
        ),
        3.4
    );

SELECT
    assert_equals(
        convert_to_number(
            '1.5m',
            0
        ),
        1.5
    );

SELECT
    assert_equals(
        convert_to_number(
            '6.6 m',
            0
        ),
        6.6
    );

SELECT
    assert_equals(
        convert_to_number(
            'abc',
            0
        ),
        0
    );