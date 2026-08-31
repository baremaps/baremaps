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
baremaps map create --map map.js

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
  --map 'map.js'
```

## Editing the map

`map.js` holds a few properties of the map as a whole and its layers, in the
order they are painted. Baremaps derives both
[the style](https://maplibre.org/maplibre-style-spec/) and
[the tileset](https://github.com/mapbox/tilejson-spec/tree/master/2.2.0) from
it.

A layer is a MapLibre style layer that also says where its features come from:

```
export default {
    id: 'building',
    type: 'fill',
    sourceLayer: 'building',
    sourceQueries: [
        {minzoom: 13, maxzoom: 20, from: 'osm_building'},
    ],
    sourceSchema: 'layers/building/create.sql',
    paint: { /* ... */ },
};
```

Everything about a subject therefore lives with it: open `layers/building/` to
work on buildings. A source layer usually feeds several layers — nine draw from
`highway`, seven from `point` — so exactly one of them carries `sourceQueries`,
and the compiler refuses a source layer declared twice or not at all.

A query names the relation and, optionally, a `filter`, written in the same
expression language the style filters in:

```
sourceQueries: [
    {minzoom: 6, maxzoom: 10, from: 'osm_waterway',
     filter: ['in', ['get', 'waterway'], ['literal', ['river', 'stream']]]},
],
```

Baremaps turns that into sql, and into sql that uses the index: every test of a
value is emitted with a `tags ? 'key'` containment test beside it, which is
redundant to the meaning and decisive to the plan. Without it the gin index
cannot answer the query, which on a country extract is the difference between a
plan costing 130 thousand and one costing 381 thousand. `where` remains for a
predicate the expression language cannot say.

`drawable: true` drops features carrying none of the attributes the style reads
at that zoom. They can only be drawn as nothing, and the set to test is the one
already derived for the projection, so this is not a filter that has to be kept
in step by hand.

A query does not name the attributes to select: those are read off the layers that use them, per zoom
level, so a tile carries what is drawn and nothing else. Adding a directive that
reads a new tag is enough to make the tiles carry it, and removing the last
directive that reads one is enough to make them stop.

The keys this format defines are camelCase; the ones that pass through to a
specification keep its spelling, which is why the property names inside `layout`
and `paint` are untouched and the style is written back out with `source-layer`.
A layer pasted from a style is refused rather than quietly ignored.

Anything the compiler can work out is left out. No layer names the source it
reads, because there is only one; the style version, the `sources` block and the
zoom range of each query are filled in the same way. What cannot be derived
stays declared: `simplify`, a tolerance in tile pixels, and `feature_ids`, off by
default because a style cannot draw with a feature identifier and identifiers
compress badly.

`baremaps map compile --map map.js --style style.json --tileset tileset.json`
writes out what was derived, which is useful for reviewing it.

The tileset format that `map.js` replaces is still accepted: pass `--tileset`
and `--style` instead of `--map`.

<details>
<summary>The tileset format</summary>

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

</details>

## Selecting a theme

The colours of the style are held in `themes/`, separately from the rules that use
them. `themes/default.js` lists every colour; the others are derived from it by a
transform, so `themes/dark.js` is the light theme inverted and the colour-vision
themes apply the corresponding confusion matrix. Deriving them means a colour
added to the default theme appears in all of them.

`BAREMAPS_THEME` selects the one the style is built with, naming a file in
`themes/` without its extension. It defaults to `default`, and an unknown name
fails with the list of valid ones.

```
BAREMAPS_THEME=dark baremaps map dev \
  --map 'map.js'
```

## JavaScript as a configuration language

All the configuration files are written in JavaScript instead of JSON.
This allows for more flexibility and the use of JavaScript functions to generate the configuration.
Additionally, it allows for imports and comments, which are not supported in JSON.
As the configuration files got bigger and more complex, this choice became more and more beneficial.

## Validating the configuration

Because the configuration is plain JavaScript, a mistyped theme key is not an error:
it evaluates to `undefined`, the property is dropped from the generated layer, and
MapLibre falls back to its own default, which is black for a line or fill colour.
The following command checks for that and for the other mistakes the language does
not catch on its own.

```
node validate.js
```

It reports as errors:

* a `theme.*` key that is read by a layer but not defined,
* a theme value that is neither a colour nor a `[zoom, value, ...]` stop array,
  or a colour that does not survive a round-trip through `utils/color.js` and so
  would be silently dropped by any theme derived from it,
* a filter that can never match, such as an `all` requiring one tag to hold two
  different values,
* a directive whose filter repeats an earlier one in the same layer, which a
  `case` expression can never reach,
* a duplicate layer id,
* anything the [MapLibre style specification](https://maplibre.org/maplibre-style-spec/)
  rejects.

It reports as warnings a theme key that nothing references, a layer module that
`map.js` does not import, and any divergence between the layer groups listed in
`parityGroups`.

That last check is worth explaining. The road, tunnel and bridge layers repeat
their directive lists rather than sharing a generated one, so that a single road
class can be adjusted without disturbing the others. The cost of that choice is
that the lists drift apart unnoticed. Rather than remove the duplication, the
check reports a feature class that some members of a group style and others do
not, and a divergence that is intended can be recorded in the group's `accept`
list so that the decision stays visible.

The MapLibre check needs its dependency; the rest run without one.

```
npm install
```

`BAREMAPS_THEME` applies here too, so a theme can be checked before it is used.
The theme values themselves are checked for every theme on every run, since a
derived theme silently keeps its parent's value for any colour it cannot parse.

```
BAREMAPS_THEME=dark node validate.js
```

## Tools

* [Overpass turbo](https://overpass-turbo.eu/) from [taginfo](https://taginfo.openstreetmap.org/)

## Contributing

As a lot of work remains to be done, contributions and feedbacks are welcome.
