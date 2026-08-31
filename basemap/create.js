/**
 Licensed under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/
import config from "./config.js";

export default {
    "steps": [
        {
            "id": "create",
            "needs": [],
            "tasks": [
                // Initialization
                "queries/initialize.sql",
                "queries/functions.sql",

                // OpenStreetMap
                "layers/header/create.sql",
                "layers/node/create.sql",
                "layers/way/create.sql",
                "layers/relation/create.sql",
                "layers/member/create.sql",
                "layers/linestring/create.sql",
                "layers/aerialway/create.sql",
                "layers/aeroway/create.sql",
                "layers/amenity/create.sql",
                "layers/attraction/create.sql",
                "layers/barrier/create.sql",
                "layers/boundary/create.sql",
                "layers/building/create.sql",
                "layers/highway/create.sql",
                "layers/landuse/create.sql",
                "layers/leisure/create.sql",
                "layers/man_made/create.sql",
                "layers/natural/create.sql",
                "layers/point/create.sql",
                "layers/power/create.sql",
                "layers/railway/create.sql",
                "layers/route/create.sql",
                "layers/tourism/create.sql",
                "layers/waterway/create.sql",
            ].map(file => {
                return {
                    "type": "ExecuteSqlScript",
                    "file": file,
                    "database": config.database,
                }
            })
        },
    ]
}
