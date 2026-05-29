# FASE 5: Databases & Webhooks Integration

## Status: 🚧 IN PROGRESS

Integration of databases and event-driven webhooks into the processing pipeline.

## Architecture Overview

### Event-Driven Processing

Components publish events via `EventDispatcher`:

```
Component (Carver, Parser, Task)
         ↓
   publishEvent("item.extracted", {...})
         ↓
   EventDispatcher
   ├─ Local Handlers (sync)
   │  └─ PostProcessingTasks listening
   └─ Remote Webhooks (async)
      └─ HTTP POST with retry logic
```

### Database Pattern

Components create databases and make them available to other tasks:

```
Plugin Component (Carver, Database Provider)
         ↓
   Creates database file
   [caseOutputDir]/plugins/[componentId]/[dbId].db
         ↓
   Publishes "database.created" event
         ↓
   PostProcessingTask listens
         ↓
   Opens connection via getRemoteDatabase()
         ↓
   Processes/indexes data
```

## Components Implemented

### 1. SQLiteDatabaseProvider

**File:** `iped-engine/src/main/java/iped/engine/database/impl/SQLiteDatabaseProvider.java`

Provides SQLite database creation and connection management.

Features:
- Automatic initialization of database files
- Thread-safe connection pooling
- JDBC-based queries, inserts, updates, deletes
- Table creation with column definitions

**Usage:**
```java
DatabaseProvider provider = new SQLiteDatabaseProvider();
Database db = provider.createComponent();
db.initialize(Paths.get("/path/to/db.db"));
DatabaseConnection conn = db.openConnection();
conn.insert("items", Map.of("path", "/file/path", "hash", "abc123"));
```

**ServiceLoader Registration:**
File: `iped-engine/src/main/resources/META-INF/services/iped.engine.database.spi.DatabaseProvider`
```
iped.engine.database.impl.SQLiteDatabaseProvider
```

### 2. PostProcessingTask

**File:** `iped-engine/src/main/java/iped/engine/task/PostProcessingTask.java`

Abstract base class for tasks that react to events published by other components.

**Features:**
- `onPluginEvent(componentId, eventType, handler)` - Listen for events from specific component
- `onEvent(eventType, handler)` - Listen for events from any component
- `getRemoteDatabase(componentId, dbId)` - Access databases created by other components
- `publishEvent(eventType, data)` - Publish events for downstream tasks

**Usage Example:**
```java
public class MyPostProcessingTask extends PostProcessingTask {
    @Override
    protected void init(Map config) throws Exception {
        // Listen for items extracted by our carver
        onPluginEvent("my-carver", "item.extracted", event -> {
            String itemPath = (String) event.data().get("path");
            // Process extracted item
        });
        
        // Listen for database creation
        onEvent("database.created", event -> {
            String componentId = (String) event.data().get("componentId");
            if ("my-carver".equals(componentId)) {
                PluginDatabase db = getRemoteDatabase(componentId, "extracted-items");
                try (DatabaseConnection conn = db.openConnection()) {
                    ResultSet rs = conn.query("SELECT * FROM items");
                    // Process results
                }
            }
        });
    }
}
```

## Standard Events

Components should publish these standard events:

| Event | Source | Data | Purpose |
|-------|--------|------|---------|
| `task.started` | Task | `{taskId, taskName}` | Task execution started |
| `task.completed` | Task | `{taskId, duration}` | Task finished successfully |
| `task.failed` | Task | `{taskId, error}` | Task failed with error |
| `item.extracted` | Carver | `{path, size, type}` | Item extracted by carver |
| `item.parsed` | Parser | `{itemHash, mimeType}` | Item parsed successfully |
| `item.indexed` | Indexer | `{itemHash, docCount}` | Item indexed in Elasticsearch |
| `database.created` | Component | `{componentId, dbId, path}` | Database created and ready |

## Integration Points

### 1. ConfigurationManager

Must initialize ComponentRegistry and store it in CaseData:

```java
ComponentTaskLoader loader = new ComponentTaskLoader();
ComponentRegistry registry = loader.load(pluginConfig, registryIndex);
caseData.putCaseObject("__iped_component_registry__", registry);
```

### 2. AbstractTask

Gains access to ComponentRegistry and EventDispatcher:

```java
protected void publishEvent(String eventType, Map<String, Object> data) throws Exception {
    EventDispatcher dispatcher = (EventDispatcher) caseData
        .getCaseObject("__iped_event_dispatcher__");
    if (dispatcher != null) {
        dispatcher.dispatch(new PluginEvent(eventType, this.getClass().getName(), data));
    }
}
```

### 3. Task Pipeline

PostProcessingTasks should be executed as a final phase:

```
[Phase 1: Regular Tasks] (Carving, Parsing, Hashing, etc)
         ↓
[Phase 2: Post-Processing Tasks] (Event consumers, database processors)
         ↓
[Webhooks] (Remote HTTP callbacks, async)
```

## Testing Strategy

### Unit Tests

- **EventDispatcherTest** - Local handlers, remote webhooks, retry logic
- **SQLiteDatabaseTest** - CRUD operations, queries, table creation
- **PostProcessingTaskTest** - Event registration, handler invocation

### Integration Tests

```java
@Test
public void testComponentDatabaseWorkflow() {
    // 1. Carver extracts and creates database
    CarverProvider carver = new TestCarverProvider();
    Carver instance = carver.createComponent();
    // carving produces items...
    // carver publishes: database.created event
    
    // 2. PostProcessingTask listens and processes
    PostProcessingTask ppTask = new TestPostProcessingTask();
    ppTask.init(Map.of());
    // ppTask listens for database.created
    // ppTask calls getRemoteDatabase()
    // ppTask queries database
    
    // 3. Verify database was processed
    assertTrue(ppTask.processedItems() > 0);
}
```

### Regression Tests

- Existing carvers produce identical output
- Database files are created correctly
- Events are dispatched in correct order
- No performance degradation

## File Structure

```
iped-engine/
├── src/main/java/iped/engine/database/impl/
│   └── SQLiteDatabaseProvider.java         (~150 lines)
├── src/main/java/iped/engine/task/
│   └── PostProcessingTask.java             (~200 lines)
└── src/main/resources/META-INF/services/
    └── iped.engine.database.spi.DatabaseProvider
```

## Success Criteria

✅ SQLiteDatabaseProvider creates and manages databases
✅ PostProcessingTask allows listening to events
✅ Remote database access works via getRemoteDatabase()
✅ Standard events are published correctly
✅ EventDispatcher integrates with task pipeline
✅ No breaking changes to existing code
✅ Zero new external dependencies

## What's Next (Phase 6)

### Metadata & Categories
- MetadataRegistry implementation
- Elasticsearch integration
- Category merge logic
- FileCategoryProvider integration

## Notes

- Database files are stored at: `[caseOutputDir]/plugins/[componentId]/[dbId].db`
- Events use CaseData.objectMap to store EventDispatcher lazily
- ComponentRegistry must be initialized before tasks run
- PostProcessingTasks should not process items directly (no processItem override)
- All database access should be through DatabaseConnection interface (thread-safe)

---

**FASE 5 STATUS**: 🚧 In Progress

Completed:
- [x] SQLiteDatabaseProvider
- [x] PostProcessingTask abstract class
- [x] Meta-INF/services registration

Remaining:
- [ ] Additional database providers (PostgreSQL, MySQL)
- [ ] EventDispatcher integration tests
- [ ] Standard event publishing throughout pipeline
- [ ] Documentation and examples

