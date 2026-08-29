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

**`Memory`** hides the segment arithmetic. Segments are a power of two in size, so a position
splits into a segment index and an offset with a shift and a mask. Segments are allocated
lazily and are always zero-filled; the header is a small extra buffer where a collection persists
its own metadata (size, end position, schema). Backings: `OnHeapMemory`, `OffHeapMemory`,
`MemoryMappedFile`, `MemoryMappedDirectory` (one file per segment, the choice for large data).

**`DataType`** reads and writes a value at an absolute position. Values are never null, and an
encoding is self-delimiting: `size(buffer, position)` recovers the size from the first bytes and
is never 0 for a written value. That invariant is what lets a collection tell written space from
never-written space. `FixedSizeDataType` and `MemoryAlignedDataType` (a power-of-two size) are
what the array-like collections need.

**Collections**

| Class | Use when |
|---|---|
| `AppendOnlyLog` | Variable-size values, addressed by the position returned on append. |
| `MemoryAlignedDataList` | Fixed power-of-two values, index → position is a shift. The default index. |
| `FixedSizeDataList` | Fixed values of any size. |
| `IndexedDataList` | Variable-size values with `long` indexes: an aligned index over a log. |
| `MonotonicDataMap` | Keys inserted in increasing order (OSM ids). Sorted keys, chunked binary search. |
| `MemoryAlignedDataMap` | Dense `long` keys; a flat array, so every key below the bound "exists". |
| `DirectHashDataMap` | Sparse, unordered keys; open addressing with a fixed capacity. |
| `IndexedDataMap` | Keys fit in the heap, values do not. |

`DataConversions` gives live `java.util` views over these and back.

## Rules that keep it robust

- A value never crosses a segment boundary. `AppendOnlyLog` starts the next segment instead,
  leaving a zero-filled tail; `Memory.write` throws if asked to cross.
- Collections persist what they need to be reopened (size, end position) in the memory header
  on `close()` / `flush()`. Reopen a memory-mapped collection with the same `DataType` and memory
  parameters. `AppendOnlyLog.HEADER_BYTES` is reserved; other users of the header write after it.
- `close()` is idempotent and a closed `Memory` rejects further access rather than touching
  unmapped buffers. `clear()` deletes the storage and leaves the collection reusable.
- Thread safety is per class and stated in its javadoc. Appends are generally safe to run
  concurrently; reads of a position are safe once the append that produced it has returned.
- Every implementation passes the same contract tests (`DataListContractTest`,
  `DataMapContractTest`) over every memory, plus `PersistenceTest` for close-and-reopen. Add a
  new implementation to those providers before anything else.
