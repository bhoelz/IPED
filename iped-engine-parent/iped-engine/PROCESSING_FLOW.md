# IPED Data Processing Flow

End-to-end flow in IPED, from data source intake to persistence:

1. Case orchestration
- Processing starts in `Manager`, which prepares output, opens the index, and starts workers plus producers: [Manager.java](src/main/java/iped/engine/core/Manager.java).
- The case final index is stored at `output/index`.

2. How a data source is used
- `ItemProducer` picks the proper reader (`SleuthkitReader`, `IPEDReader`, `UfedXmlReader`, `AD1DataSourceReader`, `FolderTreeReader`) via `isSupported`, then calls `read(source)`: [ItemProducer.java](src/main/java/iped/engine/datasource/ItemProducer.java).
- The base abstraction is `DataSourceReader` (`read`, `listOnly`, `caseData`, `output`), now in `iped-engine-core`: [DataSourceReader.java](../iped-engine-core/src/main/java/iped/engine/datasource/DataSourceReader.java).
- Each concrete source creates a `DataSource` (with UUID) and attaches it to items: [DataSource.java](../iped-engine-core/src/main/java/iped/engine/data/DataSource.java), `FolderTreeReader.java`, `SleuthkitReader.java` (in `iped-sleuthkit`), `UfedXmlReader.java` (in `iped-ufed`).

3. Item creation
- Readers instantiate `Item`, set `dataSource` and `idInDataSource`, update discovery counters (`incDiscoveredEvidences/Volume`), and enqueue.
- The queue receives an end marker (`[queue-end]`) when production is done: [ItemProducer.java](src/main/java/iped/engine/datasource/ItemProducer.java).

4. Item processing
- `ProcessingQueues` computes/updates `trackId` and ID before queue insertion (`Util.calctrackIDAndUpdateID`), including resume support (`--continue`): [ProcessingQueues.java](src/main/java/iped/engine/core/ProcessingQueues.java).
- `Worker` pulls from the queue and runs the task chain (`firstTask.processAndSendToNextTask`): [Worker.java](src/main/java/iped/engine/core/Worker.java).
- The chain is assembled by `TaskInstaller`; task order comes from configuration: [TaskInstaller.java](src/main/java/iped/engine/task/TaskInstaller.java).
- In `AbstractTask`, each task processes and forwards the item; subitems can be re-enqueued/prioritized: [AbstractTask.java](src/main/java/iped/engine/task/AbstractTask.java).

5. Where results are written
- Lucene index: `IndexTask` converts `IItem` into `Document` (`IndexItem.Document`) and writes using `writer.addDocuments(...)` — now in `iped-tasks-storage-index`: [IndexTask.java](../../../iped-tasks/iped-tasks-storage-index/src/main/java/iped/engine/task/index/IndexTask.java).
- Extracted/binary artifacts: `ExportFileTask` writes to filesystem (`extractDir`) and/or SQLite storage (`configureSQLiteStorage`, `insertIntoStorage`) — now in `iped-tasks-forensics`: [ExportFileTask.java](../../../iped-tasks/iped-tasks-forensics/src/main/java/iped/engine/task/ExportFileTask.java).
- Auxiliary metadata and commits: `Manager` performs periodic and final commits for index + storages + CSV + metadata.
- If a temporary index is used, it is moved/copied to the final `output/index`.

Summary: a data source enters through a `DataSourceReader`, becomes an `Item` with identity (`dataSource + idInDataSource + trackId`), moves through a parallel task pipeline, and is persisted primarily in the Lucene index (`output/index`) plus extraction/export artifacts (filesystem/SQLite/CSV).

> Note: this flow was originally documented when `IndexTask`/`ExportFileTask`/`DataSourceReader` lived directly under `iped-engine`. The engine has since been split into `iped-engine-core`, `iped-engine`, and per-feature `iped-tasks-*` modules — paths above reflect the current (post-split) locations. Exact line numbers from the original version of this doc were dropped since they're no longer reliable after the move; the class names and flow itself are unchanged.