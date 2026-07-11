package iped.tasks.cli;

import iped.configuration.Configurable;
import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.task.AbstractTask;
import iped.engine.task.additional.AdditionalTaskWorker;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a single {@code AbstractTask} standalone against a list of items,
 * mirroring the wiring {@code iped.engine.task.additional.AdditionalTaskRunner}
 * uses for post-indexing task re-runs (minimal {@link AdditionalTaskWorker},
 * reflective instantiation, {@code setWorker}/{@code setNextTask}/{@code init}/
 * {@code processAndSendToNextTask}/{@code finish}) -- adapted to build items
 * from disk (see {@link StandaloneItemFactory}) instead of an {@code IPEDSource}.
 */
public final class TaskExecutor {

    private TaskExecutor() {
    }

    public static RunResult run(String taskClassName, List<IItem> items, File confDir) {
        return run(taskClassName, items, confDir, null);
    }

    /**
     * @param childrenDir when non-null and {@code taskClassName} is one of
     *                    {@link TaskCompatibility#CARVING_FAMILY}, carved children
     *                    are written here (one level of extraction, no recursive
     *                    re-processing -- see {@link CarvingOutputWorker}) instead
     *                    of failing as UNSUPPORTED.
     */
    public static RunResult run(String taskClassName, List<IItem> items, File confDir, File childrenDir) {
        TaskCompatibility.Classification classification = TaskCompatibility.classify(taskClassName);
        if (classification.status() == TaskCompatibility.Status.KNOWN_UNSUPPORTED) {
            return new RunResult(taskClassName, describeInput(items), unsupportedForAll(items, classification.reason()));
        }

        AbstractTask task;
        try {
            Class<?> clazz = Class.forName(taskClassName);
            task = (AbstractTask) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | ClassCastException e) {
            String reason = "task class not found or lacks a public no-arg constructor: " + e;
            return new RunResult(taskClassName, describeInput(items), errorForAll(items, reason));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return new RunResult(taskClassName, describeInput(items), errorForAll(items, "could not instantiate task: " + e));
        }

        CollectingSinkTask sink = new CollectingSinkTask();
        AdditionalTaskWorker worker = (childrenDir != null && TaskCompatibility.CARVING_FAMILY.contains(taskClassName))
                ? new CarvingOutputWorker(0, confDir, childrenDir)
                : new AdditionalTaskWorker(0, confDir);
        task.setWorker(worker);
        task.setNextTask(sink);
        sink.setWorker(worker);

        try {
            registerTaskConfigurables(task);
            task.init(ConfigurationManager.get());
        } catch (Throwable t) {
            Failure f = classifyFailure(t);
            return new RunResult(taskClassName, describeInput(items), fixedResultForAll(items, f, 0));
        }

        List<ItemResult> results = new ArrayList<>(items.size());
        for (IItem item : items) {
            long start = System.currentTimeMillis();
            try {
                task.processAndSendToNextTask(item);
                long elapsed = System.currentTimeMillis() - start;
                results.add(ItemResult.ok(item.getPath(), elapsed, sink.takeMetadata(), sink.takeExtraAttributes()));
            } catch (Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                Failure f = classifyFailure(t);
                results.add(new ItemResult(item.getPath(), f.status(), f.message(), elapsed, null, null));
            }
        }

        try {
            task.finish();
        } catch (Exception ignored) {
            // best-effort cleanup; per-item results were already captured
        }

        return new RunResult(taskClassName, describeInput(items), results);
    }

    private static void registerTaskConfigurables(AbstractTask task) throws Exception {
        ConfigurationManager cm = ConfigurationManager.get();
        for (Configurable<?> configurable : task.getConfigurables()) {
            cm.addObject(configurable);
            cm.loadConfig(configurable);
        }
    }

    private static Failure classifyFailure(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root instanceof UnsupportedOperationException) {
            return new Failure(ItemResult.UNSUPPORTED, "task creates child items: " + root.getMessage());
        }
        if (root instanceof NullPointerException) {
            return new Failure(ItemResult.UNSUPPORTED,
                    "task appears to require case/index state unavailable standalone (" + root + ")");
        }
        return new Failure(ItemResult.ERROR, t.toString());
    }

    private static List<ItemResult> unsupportedForAll(List<IItem> items, String reason) {
        List<ItemResult> results = new ArrayList<>(items.size());
        for (IItem item : items) {
            results.add(ItemResult.unsupported(item.getPath(), reason));
        }
        return results;
    }

    private static List<ItemResult> errorForAll(List<IItem> items, String reason) {
        List<ItemResult> results = new ArrayList<>(items.size());
        for (IItem item : items) {
            results.add(ItemResult.error(item.getPath(), reason, 0));
        }
        return results;
    }

    private static List<ItemResult> fixedResultForAll(List<IItem> items, Failure f, long elapsedMs) {
        List<ItemResult> results = new ArrayList<>(items.size());
        for (IItem item : items) {
            results.add(new ItemResult(item.getPath(), f.status(), f.message(), elapsedMs, null, null));
        }
        return results;
    }

    private static String describeInput(List<IItem> items) {
        return items.size() + " item(s)";
    }

    private record Failure(String status, String message) {
    }
}
