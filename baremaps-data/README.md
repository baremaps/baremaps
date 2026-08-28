# Data Module

The data module contains classes and functions to load and process large data sets.

## On-heap, off-heap, and memory-mapped data

The following packages are used to deal with large data sets:
- `com.baremaps.data.memory` contains utilities to deal with on-heap, off-heap, and memory-mapped memories that excapes the garbage collector.
- `com.baremaps.data.type` contains classes to serialize and deserialize data types in memories.
- `com.baremaps.data.collection` defines large collections backed by memories that can hold more than `Integer.MAX_VALUE` elements.
- `com.baremaps.data.algorithm` contains algorithms to process large collections.

## Data Storage

A common issue is to deal with large data sets whose structure is discovered at runtime.
The `com.baremaps.data.storage` package contains classes to deal with this kind of data.
The following diagram describes the main interfaces and classes of this package.

```mermaid
classDiagram
    class DataColumn {
        +String name()
        +Type type()
    }
    class DataSchema {
        +String name()
        +List<DataColumn> columns()
        +DataRow createRow()
    }
    class DataRow {
        +DataSchema schema()
        +List<?> values()
        +Object get(String column)
        +Object get(int index)
        +void set(String column, Object value)
        +void set(int index, Object value)
        +DataRow with(String column, Object value)
        +DataRow with(int index, Object value)
    }
    class DataTable {
        +DataSchema schema()
        +boolean add(DataRow row)
        +void clear()
        +long size()
        +Iterator<DataRow> iterator()
    }
    class DataStore {
        +List<String> list()
        +DataTable get(String name)
        +void add(DataTable table)
        +void add(String name, DataTable table)
        +void remove(String name)
    }
    DataStore --> DataTable : has
    DataTable --> DataRow : has
    DataTable --> DataSchema : follows
    DataRow --> DataSchema : follows
    DataSchema --> DataColumn : has
```

## Executing SQL queries in the JVM

The `com.baremaps.data.calcite` package contains classes to execute SQL queries in the JVM.
This is a work in progress that demonstrates how to use the spatial extension of Apache Calcite in Apache Baremaps.
The long term goal is to perform very fast data processing without the need of a database.

