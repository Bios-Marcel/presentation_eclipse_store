---
title: Eclipse Store
sub_title: A relational in-memory database that doesn't use SQL
theme:
  name: light
  override:
    footer:
      style: template
      center: https://github.com/Bios-Marcel/presentation_eclipse_store
      right: "{current_slide} / {total_slides}"
---

Concept
==

<!-- pause -->
* No query language, CRUD via plain Java
<!-- pause -->
* Data is stored in arbitrary storage backends
<!-- pause -->
  * SQL
  * key-value store (Mongo, Redis, ...)
  * files (filesystem)
  * Blob storages (aws, minio, ...)
  * ...
<!-- pause -->
* No database server, everything is in-process
<!-- pause -->
* Have as much data as possible in-memory
<!-- pause -->
* Replaces the need for a separate ORM

<!-- end_slide -->

Pros
==

<!-- pause -->
* **You have almost total control over everything that happens**
<!-- pause -->
* Extremly fast read access
<!-- pause -->
* Rather fast writes
<!-- pause -->
* Querying via Java, instead of SQL
  * It's hard to write slow code
  * Beginners can be onboarded easily
<!-- pause -->
* Built-in lazy loading of fields and collections
<!-- pause -->
* Replaces complex ORMs / cache layers
<!-- pause -->
* The code *CAN* be super simple (more on that later)

Cons
==

<!-- pause -->
* **You have almost total control over everything that happens**
<!-- pause -->
* No traditional transactions on the domain object level
<!-- pause -->
* No traditional database constraints
<!-- pause -->
* No built-in locking
<!-- pause -->
* Any mutations affect the live data
<!-- pause -->
* No easy insight into the database contents from outside
<!-- pause -->
* Migrations are somewhat harder over time

<!-- end_slide -->

Basic Overview of Concepts
==

<!-- pause -->
* No tables, just Java objects
<!-- pause -->
* Custom binary serialisation format
  * Types are parsed upon server start and are written into a "type dictionary"
  * written data is chunked into binary blobs
  * Data is automagically migrated if possible
<!-- pause -->
* Simple `store(...), commit(), reload(...)` API
<!-- pause -->
* Background threads that manage lazy data references
<!-- pause -->
* Background threads that manage garbage in the persistent storage (due to deletions / copy on write)

<!-- end_slide -->

Comparison to SQL - The Schema
==

BEWARE, PSEUDO CODE *(Jokes on you, that's just my excuse for invalid code)*

<!-- column_layout: [10,1,10] -->

<!-- column: 0 -->

```sql
CREATE SCHEMA 'schema';

CREATE TABLE 'user' (
  'id' UUID PRIMARY KEY,
  'name' TEXT);
CREATE TABLE 'note' (
  'id' UUID PRIMARY KEY, 
  'user_id' UUID FOREIGN KEY('user'.'id'),
  'name' TEXT,
  'content' TEXT);
```

<!-- column: 1 -->

VS

<!-- column: 2 -->

```java
class Schema {
  class User {
    UUID id;
    String name;
    List<Note> notes;
  }
  class Note {
    UUID id;
    String name;
    String content;
  }

  List<User> users;
}
```
<!-- reset_layout -->

<!-- end_slide -->

Comparison to SQL - Inserting Data
==

BEWARE, PSEUDO CODE *(Jokes on you, that's just my excuse for invalid code)*

<!-- column_layout: [15,1,10] -->

<!-- column: 0 -->

```sql
BEGIN TRANSACTION;

INSERT INTO 'user' (user_id, 'marcel');
INSERT INTO 'notes' (note_id, user_id, 'TODO', 'Prepare the talk ASAP!')

COMMIT TRANSACTION;
```

```java
connection.beginTransaction();

var userId = UUID.random();
connection
        .prepareStatement("INSERT INTO 'user' ($1, $2)")
        .put("$1", userId)
        .put("$2", "marcel")
        .execute();

connection
        .prepareStatement("INSERT INTO 'note' ($1, $2, $3, $4)")
        .put("$1", UUID.random())
        .put("$2", userId)
        .put("$3", "TODO")
        .put("$4", "Prepare the talk ASAP!")
        .execute();

connection.commit();
```

<!-- column: 1 -->

VS

<!-- column: 2 -->

```java
var user = new User(
        UUID.random(),
        "marcel"
);
user.notes().add(
        new Note(
            UUID.random(),
            user.id,
            "TODO",
            "Prepare the talk ASAP!"
        )
);
schema.users.add(user);

var storer = manager.createStorer();
storer.store(schema.users);
storer.commit();
```

<!-- reset_layout -->

<!-- end_slide -->

Comparison to SQL - Rollbacks
==

BEWARE, PSEUDO CODE *(Jokes on you, that's just my excuse for invalid code)*

<!-- column_layout: [10,1,10] -->

<!-- column: 0 -->

```java
connection.beginTransaction();

var userId = UUID.random();
connection
        .prepareStatement("INSERT INTO 'user' ($1, $2)")
        .put("$1", userId)
        .put("$2", "marcel")
        .execute();

connection
        .prepareStatement("INSERT INTO 'note' ($1, $2, $3, $4)")
        .put("$1", UUID.random())
        .put("$2", userId)
        .put("$3", "TODO")
        .put("$4", "Prepare the talk ASAP!")
        .execute();

if (validationFails()) {
    connection.rollback();
    return;
}

connection.commit();
```

<!-- column: 1 -->

VS

<!-- column: 2 -->

```java
var user = new User(
        UUID.random(),
        "marcel"
);
user.notes().add(
        new Note(
            UUID.random(),
            user.id,
            "TODO",
            "Prepare the talk ASAP!"
        )
);
schema.users.add(user);

if (validationFails()) {
    var reloader = manager.createReloader();
    reloader.reloadDeep(schema.users);
    return;
}        

var storer = manager.createStorer();
storer.store(schema.users);
storer.commit();
```

<!-- reset_layout -->

<!-- end_slide -->


Comparison to SQL - Constraint Violations
==

BEWARE, PSEUDO CODE *(Jokes on you, that's just my excuse for invalid code)*

<!-- column_layout: [10,1,10] -->

<!-- column: 0 -->

```java
connection.beginTransaction();

var userId = UUID.random();
connection
        .prepareStatement("INSERT INTO 'user' ($1, $2)")
        .put("$1", userId)
        .put("$2", "marcel")
        .execute();

connection
        .prepareStatement("INSERT INTO 'note' ($1, $2, $3, $4)")
        .put("$1", UUID.random())
        .put("$2", UUID.random()) // <- THIS IS AN ERROR
        .put("$3", "TODO")
        .put("$4", "Prepare the talk ASAP!")
        .execute(); // <- THIS WILL THROW AND ROLLBACK

connection.commit();
```

<!-- column: 1 -->

VS

<!-- column: 2 -->

```java
var user = new User(
        UUID.random(),
        "marcel"
);
user.notes().add(
        new Note(
            UUID.random(),
            UUID.random(),  // <- THIS IS AN ERROR
            "TODO",
            "Prepare the talk ASAP!"
        )
);
schema.users.add(user);

var storer = manager.createStorer();
storer.store(schema.users);
storer.commit(); // <- SUCCESS, WE COMMITTED EVERYTHING! NO CONSTRAINT VIOLATION???
```

<!-- reset_layout -->

<!-- end_slide -->

Comparison to SQL - Type Safety
==

BEWARE, PSEUDO CODE *(Jokes on you, that's just my excuse for invalid code)*

<!-- column_layout: [10,1,10] -->

<!-- column: 0 -->

```java
var result = connection
        .prepareStatement("SELECT 'name' FROM 'user' WHERE 'id' = $1")
        .put("$1", userId)
        .query();

result.next();
int name = result.getInt("name") // <- RUNTIME ERROR
```

<!-- column: 1 -->

VS

<!-- column: 2 -->

```java
var user = schema.users.stream()
        .filter(user -> user.getId().equals(userId))
        .getFirst();

int name = user.getName(); // <- COMPILE ERROR
```

<!-- reset_layout -->

