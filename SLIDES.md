 # Eclipse Store

A relational in-memory database that doesn't use SQL

<!-- end_slide -->

## Concept

* No query language, CRUD via plain Java
* Data is stored in arbitrary storage backends
  * SQL
  * Document databases (Mongo, Redis, ...)
  * files (filesystem)
  * Blob storages (aws, minio, ...)
  * ...
* No database server, everything is in-process
* Have as much data as possible in-memory

<!-- end_slide -->

## Pros

* Extremly fast read access
* Rather fast writes
* Querying via Java, instead of SQL
  * It's hard to write slow code
  * Beginners can be onboarded easily
* Built-in lazy loading of fields and collections
* Replaces complex ORMs / cache layers
* The code *CAN* be super simple (more on that later)

## Cons

* No traditional transactions on the domain object level
* No traditional database constraints
* Any mutations affect the live data
* No easy insight into the database contents from outside
* Migrations are somewhat harder over time

<!-- end_slide -->

## Usecases

* Applications with rather small databases
* Single user applications (Desktop / companion servers)
* Read-Heavy multi user applications

<!-- end_slide -->

## Basic Overview of Concepts

* Custom binary serialisation format
  * Types are parsed upon server start and are written into a "type dictionary"
  * written data is chunked into binary blobs
  * Data is automagically migrated if possible
* Simple `store(...), commit(), reload(...)` API
* Background threads that manage lazy data references
* Background threads that manage garbage in the persistent storage (due to deletions / copy on write)

<!-- end_slide -->

## Usage in its Simples Form

BEWARE, PSEUDO CODE!

```java
final EmbeddedStorageManager manager = ...;

// Atomic operation to store everything in one "transaction".
final var storer = manager.createLazyStorer();

final var root = manager.root();

root.elementA.field = "new value";
root.elementC.field = "new value 2";

storer.store(elementA);
storer.store(elementC);

storer.commit();

elementA.field = "MUTATION!";
Reloader.New( manager.persistenceManager() ).reloadFlat(elementA);

// elementA.field == "new value";
```
