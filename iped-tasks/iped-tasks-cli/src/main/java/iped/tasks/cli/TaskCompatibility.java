package iped.tasks.cli;

import java.util.Map;
import java.util.Set;

/**
 * Static, module-local classification of which {@code AbstractTask} subclasses
 * can run standalone (no case, no Lucene index, no SQLite, no child-item
 * queue). Built by reading each task's {@code process()}/{@code init()} --
 * none of the task classes themselves were touched. This is a pre-flight gate
 * only: {@code TaskExecutor} still wraps execution in a runtime guard for
 * tasks not listed here (see {@link Status#UNCLEAR}).
 */
public final class TaskCompatibility {

    public enum Status {
        /** Only reads/writes the item's own metadata, attributes, or content stream. */
        COMPATIBLE,
        /** Known to require a case/index/DB and/or to create child items; refused before instantiation. */
        KNOWN_UNSUPPORTED,
        /** Not reviewed (or genuinely conditional, e.g. delegates to user scripts); left to the runtime guard. */
        UNCLEAR
    }

    public record Classification(Status status, String reason) {
    }

    // Reads/writes caseData, worker.writer (Lucene IndexWriter), worker.manager, a
    // SQLite connection, or IPEDSource -- would NPE or write into state that doesn't
    // exist in standalone mode.
    private static final Map<String, String> DEPENDS_ON_CASE = Map.ofEntries(
            Map.entry("iped.engine.task.index.IndexTask", "writes to worker.writer/manager.getIndexTemp()"),
            Map.entry("iped.engine.task.ExportFileTask", "opens SQLite storage connections, uses caseData"),
            Map.entry("iped.engine.task.DuplicateTask", "reads/writes caseData, uses worker.getIndexingPort()"),
            Map.entry("iped.engine.task.SkipCommitedTask", "uses worker.getIndexingPort(), reads/writes caseData"),
            Map.entry("iped.engine.task.IgnoreHardLinkTask", "requires SleuthkitInputStreamFactory/FsContent (disk-image state)"),
            Map.entry("iped.engine.task.EmbeddedDiskProcessTask", "uses Manager.getInstance() and the item queue"),
            Map.entry("iped.engine.task.TempFileTask", "reads caseData.getCaseObject(CmdLineArgs)/isIpedReport()"),
            Map.entry("iped.engine.task.MinIOTask", "reads caseData.getCaseObject(CmdLineArgs)"),
            Map.entry("iped.engine.task.index.ElasticSearchIndexTask", "reads caseData.getCaseObject(CmdLineArgs)/isIpedReport()"),
            Map.entry("iped.engine.graph.GraphTask", "uses caseData.getCaseObject()/IPEDSource/IPEDReader"),
            Map.entry("iped.engine.task.ExportCSVTask", "reads caseData.getCaseObject(CmdLineArgs)"),
            Map.entry("iped.engine.task.HTMLReportTask", "opens IPEDSource with worker.writer, accumulates into caseData"));

    // Calls worker.processNewItem(...) (or an ancestor that does), which
    // AdditionalTaskWorker deliberately throws UnsupportedOperationException for.
    // The carving family (BaseCarveTask and subclasses) is deliberately NOT listed
    // here -- see CARVING_FAMILY below, which TaskExecutor gives a real (if
    // one-level-only) path to succeed via CarvingOutputWorker.
    private static final Map<String, String> CREATES_CHILDREN = Map.ofEntries(
            Map.entry("iped.engine.task.video.VideoThumbTask", "creates frame subitems; also extends ThumbTask"),
            Map.entry("iped.engine.task.ParsingTask", "container expansion calls worker.processNewItem(); also extends ThumbTask"));

    /**
     * BaseCarveTask and its subclasses: create child items via
     * {@code worker.processNewItem()}, same as {@link #CREATES_CHILDREN}, but
     * {@code TaskExecutor} recognizes this specific family and, when
     * {@code --extract-children-to <dir>} is given, runs them with
     * {@link CarvingOutputWorker} instead of the plain
     * {@code AdditionalTaskWorker} -- redirecting each carved child to a flat
     * output directory (one level of extraction, no recursive
     * carving/parsing/hashing of the child -- see {@link CarvingOutputWorker}'s
     * javadoc). Without that flag, these still fail per-item as UNSUPPORTED via
     * the normal runtime guard, exactly like {@link #CREATES_CHILDREN} tasks.
     */
    public static final Set<String> CARVING_FAMILY = Set.of(
            "iped.engine.task.carver.BaseCarveTask",
            "iped.engine.task.carver.CarverTask",
            "iped.engine.task.carver.LedCarveTask",
            "iped.engine.task.carver.KnownMetCarveTask",
            "iped.engine.task.FragmentLargeBinaryTask");

    // Seed list of tasks read end-to-end and confirmed to only touch their own
    // item's metadata/attributes/content stream.
    private static final Set<String> COMPATIBLE_SEED = Set.of(
            "iped.engine.task.HashTask",
            "iped.engine.task.SignatureTask",
            "iped.engine.task.NamedEntityTask",
            "iped.engine.task.regex.RegexTask",
            "iped.engine.task.LanguageDetectTask",
            "iped.engine.task.EntropyTask",
            "iped.engine.task.SetCategoryTask",
            "iped.engine.task.SetTypeTask",
            "iped.engine.task.HashDBLookupTask",
            "iped.engine.task.PhotoDNATask",
            "iped.engine.task.PhotoDNALookup",
            "iped.engine.task.QRCodeTask",
            "iped.engine.task.die.DIETask",
            // Null-guarded (caseData/stats/worker.manager) specifically to work standalone --
            // see ThumbTask.getThumbFile, MakePreviewTask.makeHtmlPreviewAndStore,
            // HashTask/ImageThumbTask/DocThumbTask stats guards,
            // ImageThumbTask.accum()'s standalone fallback,
            // AbstractTranscriptTask.finish/GoogleTranscriptTask.init/
            // MicrosoftTranscriptTask.init/RemoteTranscriptionTask for the guards.
            "iped.engine.task.ThumbTask",
            "iped.engine.task.DocThumbTask",
            "iped.engine.task.ImageThumbTask",
            "iped.engine.task.MakePreviewTask",
            "iped.engine.task.transcript.AudioTranscriptTask");

    // Compatible, but whose actual behavior/support depends on external config or
    // connectivity this classifier can't evaluate statically -- surfaced as an
    // extra note (e.g. in --list-tasks) rather than blocking or hiding the task.
    private static final Map<String, String> COMPATIBLE_WITH_NOTE = Map.ofEntries(
            Map.entry("iped.engine.task.ScriptTask",
                    "runs a user-supplied JS script (init/process/finish); standalone support depends entirely on what that script does, not on this class"),
            Map.entry("iped.engine.task.PythonTask",
                    "runs a configured Python script via Jep (one instance per script in TaskInstaller.toml, e.g. NSFWNudityDetectTask.py, FaceRecognitionTask.py); standalone support depends on that script, not on this class, and requires Jep/Python to be installed"),
            Map.entry("iped.engine.task.RemoteImageClassifierTask",
                    "delegates classification to an external HTTP service; requires that service to be reachable, independent of case/index state"));

    private static final String CARVING_NOTE = "creates child items -- pass --extract-children-to <dir> to capture "
            + "one level of carved output as flat files (no recursive re-processing of the carved children); "
            + "without that flag, each item fails as UNSUPPORTED";

    private TaskCompatibility() {
    }

    public static Classification classify(String taskClassName) {
        if (CARVING_FAMILY.contains(taskClassName)) {
            return new Classification(Status.UNCLEAR, CARVING_NOTE);
        }
        String childrenReason = CREATES_CHILDREN.get(taskClassName);
        if (childrenReason != null) {
            return new Classification(Status.KNOWN_UNSUPPORTED, "creates child items (" + childrenReason + ")");
        }
        String caseReason = DEPENDS_ON_CASE.get(taskClassName);
        if (caseReason != null) {
            return new Classification(Status.KNOWN_UNSUPPORTED, "depends on case/index (" + caseReason + ")");
        }
        if (COMPATIBLE_SEED.contains(taskClassName)) {
            return new Classification(Status.COMPATIBLE, "reads/writes only its own item's metadata/attributes/content");
        }
        String note = COMPATIBLE_WITH_NOTE.get(taskClassName);
        if (note != null) {
            return new Classification(Status.UNCLEAR, note);
        }
        return new Classification(Status.UNCLEAR,
                "not reviewed; will be attempted, with case/index and child-item errors reported as unsupported");
    }
}
