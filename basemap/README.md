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
// This command creates the database schema. It drops the tables an import writes
// into, so it refuses to run against a database that already holds data unless
// you pass --force.
baremaps map create --map map.js

// This command imports the data into the database
baremaps workflow execute --file import.js

// This command refreshes the materialized views
baremaps workflow execute --file refresh.js
```

## Updating the database

The database can periodically be updated with the following commands. 
The update workflow will download the latest changes from OpenStreetMap (osc.xml) and apply them to the database.
Refreshing the materialized views is costly, so it is a separate command. Every layer reads a
stored relation, which is what keeps a tile from repeating the work that does not depend on it,
so an update that is not followed by a refresh changes nothing a reader sees: the map stands as
it did at the last refresh, however many changes have been applied to the tables beneath it.

```
// This command updates the database
baremaps workflow execute --file update.js

// This command refreshes the materialized views
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

`map.js` describes the map once, and baremaps derives both
[the style](https://maplibre.org/maplibre-style-spec/) and
[the tileset](https://github.com/mapbox/tilejson-spec/tree/master/2.2.0) from
it. The document has three parts, and every property belongs to exactly one of
them:

```
export default {
    // How the map presents itself, in the style specification's own vocabulary.
    name: 'OpenStreetMap Vecto',
    center: [0, 0],
    zoom: 2,
    sprite: 'https://www.baremaps.com/assets/icons/icons',
    glyphs: 'https://www.baremaps.com/assets/fonts/{fontstack}/{range}.pbf',

    // The tiles the layers read, which is that specification's vector source.
    source: {
        url: 'http://localhost:9000/tiles.json',
        tiles: ['http://localhost:9000/tiles/{z}/{x}/{y}.mvt'],
        bounds: [-180, -85, 180, 85],
        minzoom: 0,
        maxzoom: 16,
        attribution: '© OpenStreetMap',
    },

    // What the tiles are built out of, which the browser never sees.
    database: 'jdbc:postgresql://localhost:5432/baremaps',
    schema: ['queries/initialize.sql', /* ... */],

    // The elevation the relief and the contours in those tiles are traced from, if the map
    // has any.
    terrain: {dem: 'data/mapterhorn.pmtiles', /* ... */},

    // The layers, bottom to top.
    layers: [background, /* ... */],
};
```

`url`, `tiles`, `bounds`, `minzoom`, `maxzoom` and `attribution` mean inside
`source` what they mean in the style specification, and are written back out
unchanged. There is one source and every layer reads it, so it needs no name of
its own: `id` defaults to `baremaps`, `minzoom` to 0 and `maxzoom` to 20, and
`featureIds` is off by default, because a style cannot draw with a feature
identifier and identifiers compress badly. These properties used to sit at the
top level, where they read as properties of the map rather than of its tiles; a
map that still declares them there is refused rather than quietly ignored.

`terrain` is not a second source. The relief and the contours are traced from
elevation rather than answered by a query, but they travel in the same tiles as
everything else, so a place arrives once with its roads. It is described under
[Terrain](#terrain).

A layer is a MapLibre style layer that also says where its features come from:

```
export default {
    id: 'building',
    type: 'fill',
    sourceLayer: 'building',
    sourceQueries: [
        {minzoom: 13, maxzoom: 20, from: 'osm_building'},
    ],
    paint: { /* ... */ },
};
```

A layer that draws more than one feature class declares them as `directives`,
one per class, and `asLayerObject` merges them into the `filter`, `layout` and
`paint` of a single style layer. They are the layer's bulk, but they belong to
it, so they are nested inside it rather than declared beside it:

```
export default asLayerObject({
    id: 'railway_tunnel',
    type: 'line',
    sourceLayer: 'railway',
    filter: ['==', ['get', 'tunnel'], 'yes'],
    directives: withSortKeys([
        {filter: ['==', ['get', 'railway'], 'subway'],
         'line-color': theme.railwayTunnelColor,
         'line-width-stops': theme.railwaySubwayLineWidth},
        // ...
    ]),
});
```

The layer's own `filter` narrows what it draws at all; each directive's `filter`
picks the class it applies to, and their order decides which wins where two
match. `withSortKeys` derives the `fill-sort-key` and `line-sort-key` that keep
that order on screen, and `withSymbolSortKeys` does the same for labels. The
wrapper is written out at each layer rather than applied for it, because which
one a layer wants does not follow from what it draws.

A layer module lives at `layers/<topic>/<name>.js` and
`map.js` imports it under the two joined by an underscore, so a binding can be
read off a path and a path off a binding. A source layer usually feeds several
layers — nine draw from `highway`, seven from `point` — so exactly one of them
carries `sourceQueries`, and the compiler refuses a source layer declared twice
or not at all.

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

An attribute nothing draws with leaves no such trace, so it is the one a layer
has to ask for. `attributes` names the tags the tiles must carry for a layer
beyond what it is drawn from:

```
export default asLayerObject({
    id: 'point_icon',
    sourceLayer: 'point',
    attributes: ['osm_id'],
    // ...
});
```

That is how a click on a point can be turned into a link to the node it was
drawn from, and how a panel shows a reference beside a name. It is asked for on
a layer rather than on a source or a query so that it is carried at the zoom
levels that layer draws at, where a reader can reach the feature, and at no
others: `point_icon` starts at 16, so `osm_id` is in the tiles from 16 and the
tiles below are unchanged. An attribute that is only carried does not make a
feature drawable either, since a feature carrying an identifier and nothing else
is still drawn as nothing.

The tags themselves come from the queries, so a column has to be a tag before it
can be carried: `queries/point.sql` writes the node's identifier into the tags as
`osm_id`. `id` is not available as a tag name, being the column the tile store
selects separately when `source.featureIds` is on. That flag is the other way to
ship identifiers, as the feature id MapLibre exposes rather than as an
attribute; it is all or nothing, applying to every layer at every zoom, where
`attributes` is asked for one layer at a time.

An identifier is the worst case for a tile, being unique, so it neither repeats
within a layer nor compresses, and it is paid once per feature: the z16 tile over
the old town of Zurich carries 1051 points and grows from 12.4 to 20.9 KB
gzipped.

### Where the sql lives

`layers/` holds JavaScript and nothing else. Every script that has to run before
the layers do lives in `queries/`, and `map.js` lists them in the order they run:

```
queries/initialize.sql   extensions
queries/functions.sql    the functions the queries call
queries/header.sql       the tables an import writes into
queries/node.sql
queries/way.sql
queries/relation.sql
queries/member.sql
queries/linestring.sql
queries/views.sql        one view per source layer
queries/point.sql        the zoom chain the point layer is read from
```

`queries/views.sql` holds one view per source layer, which is the relation a
layer names in `sourceQueries`. A generalized layer's chain of levels is named
after that relation too, so the name is the handle rather than an alias. Most of
the views are one filter on `osm_way`, and keeping them side by side rather than
one per directory is what makes a subject filtered unlike its neighbours visible
instead of buried.

The buildings at the end of that file are the one place it holds more than a
filter. A building is coloured by the land it stands on, which is not a fact
the building carries: it is a point-in-polygon against the landuse, and asking
it while a tile is built costs more than everything else the tile does. So it
is answered once into a materialized view and read back by a lookup, and the
landuse is subdivided into a second one first, because a bounding box around a
whole forest excludes nothing.

Two scripts do not fit that shape and keep their own file. `point.sql` builds
twenty views rather than one, a chain of zoom levels each holding the kinds of
point worth drawing at it. `ocean.sql` reads the tables the shapefile import
creates, so `import.js` runs it there rather than with the rest.

`queries/assertions.sql` and `queries/statistics.sql` are run by hand and belong
to no pipeline.

The keys this format defines are camelCase; the ones that pass through to a
specification keep its spelling, which is why the property names inside `layout`
and `paint` are untouched and the style is written back out with `source-layer`.
A layer pasted from a style is refused rather than quietly ignored.

Anything the compiler can work out is left out. No layer names the source it
reads, because there is only one; the style version, the `sources` block and the
zoom range of each query are filled in the same way. What cannot be derived
stays declared, such as `simplify`, a tolerance in tile pixels.

### Generalizing

A source layer too dense to draw when the map zooms out declares how it is
thinned, and baremaps builds and names the levels it is then read from. The
query above them names the relation once, `from: 'osm_leisure'`, and never a
level.

```
generalize: {
    by: 'leisure',
    values: ['park', 'pitch', 'garden', /* ... */],
},
```

Below zoom 13, neighbouring areas sharing that value are dilated until they
touch, merged, eroded back and simplified, one level reading the one above it.

Line work is thinned differently, because two roads that meet are already one
line after the first pass and merging again gains nothing. `kind: 'lines'`
merges once into a single relation that every level then simplifies out of,
dropping what is too short to see, and narrows the classes as the map zooms out:

```
generalize: {
    kind: 'lines',
    by: 'highway',
    values: ['motorway', 'trunk', 'primary', /* ... */],
    minzoom: {motorway: 4, trunk: 6, primary: 6, residential: 11, /* ... */},
},
```

`minzoom` is the lowest zoom each value is still drawn at: a motorway is worth a
pixel at zoom 4 and a residential street is not. A value with no entry is drawn
to the bottom of the chain, which is the lowest zoom the layer is queried at.

What survives and how much detail it loses stays declared, because it is a
judgement about the map. Everything else follows: that a level exists, what it
is called, that each reads the right input, and therefore the order they are
built and refreshed in. `refresh.js` refreshes the chain in dependency order
without being told about it.

`baremaps map compile --map map.js --style style.json --tileset tileset.json
--sql generalize.sql` writes out what was derived, which is useful for reviewing
it.

It is also how a change to a `generalize` block is applied to a database that is
already imported. `refresh.js` refreshes the levels, it does not redefine them,
so a value added to `values` or a zoom changed in `minzoom` does not reach the
map until the chain is rebuilt; `map create` would rebuild it, but it drops the
tables the import wrote into on the way. Compile the sql and run the statements
for the layer that changed:

```
baremaps map compile --map map.js --sql generalize.sql
grep -A100 osm_boundary generalize.sql | psql "$BAREMAPS_URL"
```

The same holds for `queries/point.sql`, which defines materialized views rather
than refreshing them: run the file itself.

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

## Terrain

The relief and the elevation contours are traced from a digital elevation model rather than
queried from the database, so they are declared beside the source rather than inside it:

```
terrain: {
    dem: 'data/mapterhorn.pmtiles',
    demMaxzoom: 12,
    minzoom: 4,
    attribution: '© Mapterhorn',
},
```

This is not a second source. The tiles the browser reads carry `hillshade` and `contour` beside the
layers the database answers with, so the relief of a place arrives with its roads, in one request,
at the same zoom, through one style source. What this block says is where they are traced from and
over which zoom levels, none of which the browser sees.

`dem` names an archive of [terrarium][terrarium] encoded raster tiles, which is what
[Mapterhorn][mapterhorn] publishes; `demMaxzoom` is the deepest level it holds, and `demTileSize`
the side of one of its tiles, which defaults to 512 because that is what Mapterhorn ships.
`minzoom` is the first zoom the terrain is traced at, below which relief is noise, and `maxzoom`
defaults to the source's: a tile that carries no terrain has none, there being no second pyramid
left for a client to stretch a shallower tile from. `attribution` is added to the source's, the
tiles being one thing to credit.

Download the archive, or an extract of it, to the path `dem` names:

```
pmtiles extract https://download.mapterhorn.com/planet.pmtiles data/mapterhorn.pmtiles \
  --bbox=5.9,45.8,10.5,47.8 --maxzoom=12
```

The whole planet is available too, and each additional zoom level roughly doubles it. `demMaxzoom`
has to match what was extracted: past it the archive is sampled beyond its resolution, which is
smooth rather than wrong, and below it the detail is downloaded and never read. A map that declares
terrain and finds no archive at that path is refused rather than served without relief.

Two layers read it, and they read it as they read anything else: the source layers `hillshade` and
`contour` are the two the terrain contributes to the tiles, and the compiler knows them by name as
the ones that owe no query.

```
export default asLayerObject({
    id: 'terrain_hillshade',
    type: 'fill',
    sourceLayer: 'hillshade',
    // ...
});
```

`hillshade` carries the shading as six nested polygons, from the broadest and faintest to the
smallest and strongest, tagged `level` 1 to 6: 1 and 2 are the lit side and 3 to 6 the shaded one.
They are drawn over one another, so a theme colour says what a level adds rather than what it ends
up as. Expressing the relief this way rather than as a shaded image is what lets the dark and the
colour-vision themes shade the map to suit themselves.

`contour` carries the elevation contours as lines, tagged with the `level` they are drawn at and,
on every fifth one, `index`. They are lines and not the boundaries of the ground above them because
only a line can be labelled with the height it stands for, which `terrain_contour_label` does for
the index contours. The interval between contours changes with the zoom level, from 1000 meters at
the top to 50 at the bottom, so which contour is an index contour is decided where the interval is
known and not by a filter in the style. The intervals are chosen for steep ground: what sets them
is how far apart the contours land on the screen, and on a mountain face the same interval draws
them far closer together than it does on a hillside.

Both are traced from a single elevation grid when a tile is asked for, and merged into the tile the
queries answer with. `map dev` traces them per request, `map serve` caches the merged tile, and
`map export` traces them into the archive it writes, so an exported map carries its own relief.
Tracing is what a terrain tile costs, and the merge is what a client no longer pays: one request
per place instead of two, and no zoom range that only half the map has.

The elevation can also be previewed on its own, from an archive or from a GeoTIFF, without a
database:

```
baremaps dem serve --pmtiles data/mapterhorn.pmtiles --pmtiles-maxzoom 12
```

[mapterhorn]: https://mapterhorn.com/data-access/
[terrarium]: https://github.com/tilezen/joerd/blob/master/docs/formats.md#terrarium

## Selecting a theme

The colours of the style are held in `themes/`, separately from the rules that use
them. `themes/default.js` lists every colour, and most of the others are derived
from it by a transform, so `themes/dark.js` is the light theme inverted and the
colour-vision themes apply the corresponding confusion matrix. Deriving them means
a colour added to the default theme appears in all of them.

`themes/soft.js` is the exception, a palette rather than a transform: a map in the
manner of the mobile maps of the last decade, with a warm off-white ground under
the towns, a bright blue for the water, yellow-greens for the open country, a road
network ranked by how dark its grey is rather than by hue, and grey labels. No transform reaches it, because the
difference is not in the colours but in what the map chooses to colour at all:
OpenStreetMap Carto gives every land use a hue of its own, where this one spends
its saturation on the water, the countryside and the points of interest and flattens
the built-up land uses into the ground, so that a label laid over it stays the
loudest thing on the screen. Its colours are sampled rather than guessed at, which
is what a screen map wants: not a drained map, a map that spends its colour in
fewer places. A palette states every colour itself, and `validate.js` reports one
that leaves a key to be inherited, that key being a single feature drawn in another
map's colours.

The hillshade colours are held out of all of this, `theme.js` taking them from the
default theme whichever theme is selected. They are not the colour of anything,
they are the light falling on it, and inverting a map does not move the sun:
derived, they would turn the lit slopes dark and the shaded ones light, which
reads as terrain pressed into the ground rather than standing out of it.

A transform has to hand back colours as distinguishable as it was given, which is
why the ones in `utils/color.js` move a colour by a fraction of the distance it
has left rather than by a fixed amount. A map is mostly pale, so a transform that
adds a fixed amount of lightness runs its colours into the top of the scale and
holds them there: the background, the minor roads and the landuse fills arrive as
one white, and the theme derived from that one inherits one black. `validate.js`
reports a theme that does this.

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
* a derived theme that pushes a colour to pure white or pure black where the
  theme it derives from held it off that end, which is how a transform loses the
  distinctions it was given,
* a filter that can never match, such as an `all` requiring one tag to hold two
  different values,
* a directive whose filter repeats an earlier one in the same layer, which a
  `case` expression can never reach,
* a duplicate layer id,
* anything the [MapLibre style specification](https://maplibre.org/maplibre-style-spec/)
  rejects.

It reports as warnings a theme key that nothing references, a layer module that
`map.js` does not import, a palette theme that leaves one of its colours to the
theme it imports, and any divergence between the layer groups listed in
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
