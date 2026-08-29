# Data Module

Collections for data sets that do not fit in the heap. A planet-sized OpenStreetMap import
holds billions of nodes; this module keeps them in off-heap or memory-mapped memory, addressed by
`long` positions, on a machine with a lot of RAM and a fast SSD. When a data set is small, the
same collections run on the heap and the trade-offs change, which is why several implementations
of each interface coexist.

## Layers

```
com.baremaps.data.memory      Memory: a growable address space of fixed-size segments
com.baremaps.data.type        DataType: how a value is encoded at a position
com.baremaps.data.collection  DataList / DataMap built on a Memory and a DataType
com.baremaps.data.algorithm   sort, search and union over those collections
```

**`Memory`** hides the segment arithmetic. Segments are `MemorySegment`s (the FFM API) of a
power-of-two size, so a position splits into a segment index and an offset with a shift and a
mask; segments may exceed 2 GB. They are allocated lazily and are always zero-filled; the header is
a small extra segment where a collection persists its own metadata (size, end position, schema).
Values go through a `DataType`; for the indexes and bitmaps a collection probes on every access,
`get(ValueLayout.OfLong, position)` and its siblings mirror the layout-typed accessors of
`MemorySegment` and skip the boxing, and `getAndSetBits` sets bits atomically.
Backings: `Memory.offHeap()` (native memory), `Memory.mappedFile(path)`, and
`Memory.mappedDirectory(path)` (one file per segment, the choice for large data). All segments of
a memory live in one `Arena`: `close()` frees or unmaps them deterministically, later accesses
throw `IllegalStateException`, and memory that is never closed is never reclaimed.

**`DataType`** reads and writes a value at an absolute `long` offset of a `MemorySegment`, with the
`ValueLayout.JAVA_*_UNALIGNED` layouts in native byte order: values are packed at arbitrary offsets,
and files are not portable across endianness. Values are never null, and an encoding is
self-delimiting: `size(segment, position)` recovers the size from the first bytes and is never 0
for a written value. That invariant is what lets a collection tell written space from
never-written space. `FixedSizeDataType` is what the array-like collections need; `DenseDataMap`
further requires a power-of-two size, so that the position of a slot is a shift.

**Collections**

The names say what the caller decides on, the shape of the values or of the keys, not how the
collection works inside.

| Class | Use when |
|---|---|
| `AppendOnlyLog` | Variable-size values, addressed by the position returned on append. |
| `FixedSizeDataList` | Fixed-size values with `long` indexes: a flat array. The default index. |
| `VariableSizeDataList` | Variable-size values with `long` indexes: a `FixedSizeDataList` of positions over an `AppendOnlyLog`. |
| `DenseDataMap` | Dense but gappy `long` keys (a planet's OSM ids): a flat array in pages allocated on demand, one bit per key for presence. The index of a world import. |
| `SparseDataMap` | Sparse `long` keys inserted in increasing order (extracts). Sorted keys, chunked binary search. |
| `VariableSizeDataMap` | Variable-size values behind either map: a map of positions over an `AppendOnlyLog`. |

`DataConversions` gives live `java.util` views over these and back.

## Rules that keep it robust

- A value never crosses a segment boundary. `AppendOnlyLog` starts the next segment instead,
  leaving a zero-filled tail; `Memory.write` throws if asked to cross.
- Collections persist what they need to be reopened (size, end position) in the memory header
  on `close()` / `flush()`. Reopen a memory-mapped collection with the same `DataType` and memory
  parameters. `AppendOnlyLog.HEADER_BYTES` is reserved; other users of the header write after it.
- `close()` is idempotent; a closed `Memory` throws on further access rather than touching
  unmapped memory. `clear()` deletes the storage and reopens the memory, so the collection stays
  usable. Every collection is `AutoCloseable`; close it, or its native memory leaks.
- Thread safety is per class and stated in its javadoc. Appends are generally safe to run
  concurrently; reads of a position are safe once the append that produced it has returned.
- Every implementation passes the same contract tests (`DataListContractTest`,
  `DataMapContractTest`) over every memory, plus `PersistenceTest` for close-and-reopen. Add a
  new implementation to those providers before anything else.

## Tests and benchmarks

Beyond the contract tests, `AppendOnlyLogBoundaryTest`, `ConcurrencyTest`, `LargePositionTest`,
`DataTypeInvariantTest` and `GeometryDataTypeTest` pin the behaviours that only show up at scale:
segment boundaries, concurrent appends, positions beyond `int`, and the size invariant of every
type. Run them with `./mvnw -pl baremaps-data test`; `./mvnw -pl baremaps-data jacoco:prepare-agent test
jacoco:report` writes a coverage report to `target/site/jacoco`.

JMH benchmarks for this module live in `baremaps-benchmarking` under
`com.baremaps.benchmarking.data`: memory backings, maps, the log, data types and the external sort.
`OsmIndexBenchmark` models the node index of a planet import (increasing, 75% dense ids, way-shaped
lookups) and is the reference for choosing the map behind `WorkflowContext`.

```
./mvnw -pl baremaps-data,baremaps-benchmarking -DskipTests install
java -jar baremaps-benchmarking/target/benchmarks.jar 'com.baremaps.benchmarking.data.*'
```

Run a subset with a class name (`MemoryBenchmark`) and compare two builds with
`-rf json -rff before.json` / `after.json`. Any change to `Memory`, the map layouts or a data type
encoding should come with a before/after run.
