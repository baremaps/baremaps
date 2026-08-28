<!--
Licensed under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# OpenStreetMap Vecto

This directory contains the configuration files for a general-purpose map based on OpenStreetMap data.
It is used to generate vector tiles and to produce a Mapbox style inspired by [OpenStreetMap Carto](https://github.com/gravitystorm/openstreetmap-carto).

## Requirements

* [Postgres](https://www.postgresql.org/) 13+
* [PostGIS](https://postgis.net/) 3+
* [Java](https://adoptium.net/) 17+
* [Baremaps](https://www.baremaps.com/) 0.7+

A PostgreSQL database with the PostGIS extension should be accessible with the following jdbc settings:

```
jdbc:postgresql://localhost:5432/baremaps?user=baremaps&password=baremaps
```

If you plan on importing the whole planet, you will need a powerful machine with a lot of storage. You may also want to modify the `postgresql.conf` file to increase some of the default settings.

```
work_mem = 4GB
shared_buffers = 4GB
maintenance_work_mem = 16GB
autovacuum_work_mem = 4GB
max_worker_processes = 16
max_parallel_workers_per_gather = 8
max_parallel_workers = 16
wal_level = minimal
checkpoint_timeout = 10min
max_wal_size = 20GB
min_wal_size = 80MB
checkpoint_completion_target = 0.9
max_wal_senders = 0
```

## Initializing the database

Assuming that the necessary requirements have been installed, the database can be populated with the following commands.

```
// This command creates the database schema
baremaps workflow execute --file create.js

// This command imports the data into the database
baremaps workflow execute --file import.js

// This command refreshes the materialized views
baremaps workflow execute --file refresh.js
```

## Updating the database

The database can periodically be updated with the following commands. 
The update workflow will download the latest changes from OpenStreetMap (osc.xml) and apply them to the database.
Refreshing the materialized views is costly and only necessary if the low zoom levels need to be updated, therefore it is optional.

```
// This command updates the database
baremaps workflow execute --file update.js

// This command refreshes the materialized views (optional)
baremaps workflow execute --file refresh.js
```

## Serving the tiles and the style in dev mode

The development server can be started with the following command.
The dev mode automatically reloads the map when the configuration files are modified, which is useful for development and testing.

```
baremaps map dev --log-level DEBUG \
  --tileset 'tileset.js' \
  --style 'style.js'
```

## Editing the tileset

The configuration format used in the `tileset.js` file extends the [TileJSON specification](https://github.com/mapbox/tilejson-spec/tree/master/2.2.0).
Simply put, it adds in the ability to describe the `vector_tiles` and their content with SQL queries that follow the PostGIS dialect.

```
{
  "tilejson": "2.2.0",
  "tiles": [
    "http://localhost:9000/tiles/{z}/{x}/{y}.mvt"
  ],
  "vector_layers": [
    {
      "id": "aerialway",
      "queries": [
        {
          "minzoom": 14,
          "maxzoom": 20,
          "sql": "SELECT id, tags, geom FROM osm_way_z${zoom} WHERE tags ? 'aerialway'"
        }
      ]
    }
  ]
}
```

## Editing the style

The configuration format used in the `style.js` file follows the [Mapbox style specification](https://github.com/mapbox/mapbox-gl-js).

## JavaScript as a configuration language

All the configuration files are written in JavaScript instead of JSON.
This allows for more flexibility and the use of JavaScript functions to generate the configuration.
Additionally, it allows for imports and comments, which are not supported in JSON.
As the configuration files got bigger and more complex, this choice became more and more beneficial.

## Tools

* [Overpass turbo](https://overpass-turbo.eu/) from [taginfo](https://taginfo.openstreetmap.org/)

## Contributing

As a lot of work remains to be done, contributions and feedbacks are welcome.
