package iped.tasks.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.config.TaskInstallerConfig;
import iped.engine.task.AbstractTask;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs a single IPED processing task standalone against an isolated file or
 * directory, without a case, Lucene index or SQLite database -- see
 * {@link ConfigBootstrap}, {@link StandaloneItemFactory}, {@link TaskExecutor}
 * for how each of those requirements is avoided without modifying any task.
 *
 * <pre>{@code
 * java -jar iped-tasks-cli.jar --conf /path/to/iped/release --task iped.engine.task.HashTask --input /path/to/file
 * java -jar iped-tasks-cli.jar --conf /path/to/iped/release --list-tasks
 *
 * // Carving family only: writes each carved child as a flat file under the given
 * // directory. This is ONE LEVEL OF EXTRACTION ONLY -- carved children are not fed
 * // back through the pipeline, so they get no recursive carving, parsing, hashing,
 * // or classification of their own (see CarvingOutputWorker's javadoc).
 * java -jar iped-tasks-cli.jar --conf /path/to/iped/release --task iped.engine.task.carver.CarverTask \
 *     --input /path/to/file --extract-children-to /path/to/carved-output
 * }</pre>
 *
 * <p>Exit code: 0 if every item succeeded, 2 if any item was unsupported,
 * 1 if any item errored (or a startup error occurred).
 */
public class StandaloneTaskCli {

    @Parameter(names = "--task", description = "Fully qualified class name of the AbstractTask to run")
    private String taskClassName;

    @Parameter(names = "--input", description = "File or directory to process", converter = FileConverter.class)
    private File input;

    @Parameter(names = "--recursive", description = "Recurse into subdirectories when --input is a directory")
    private boolean recursive = false;

    @Parameter(names = "--conf", description = "IPED release directory (contains conf/, scripts/, tools/, profiles/ as siblings)",
            required = true, converter = FileConverter.class)
    private File confDir;

    @Parameter(names = "--output-format", description = "stdout or json")
    private String outputFormat = "stdout";

    @Parameter(names = "--output-file", description = "Write output to this file instead of stdout", converter = FileConverter.class)
    private File outputFile;

    @Parameter(names = "--list-tasks", description = "List installable tasks and their standalone-compatibility status")
    private boolean listTasks = false;

    @Parameter(names = "--extract-children-to",
            description = "Carving tasks only: write each carved child as a flat file under this directory "
                    + "(ONE LEVEL OF EXTRACTION ONLY -- children are not recursively carved/parsed/hashed). "
                    + "Without this flag, carving tasks report each item as UNSUPPORTED.",
            converter = FileConverter.class)
    private File extractChildrenTo;

    @Parameter(names = { "--help", "-h" }, help = true, description = "Show usage")
    private boolean help = false;

    public static void main(String[] args) {
        StandaloneTaskCli cli = new StandaloneTaskCli();
        JCommander jc = JCommander.newBuilder().addObject(cli).programName("iped-tasks-cli").build();
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(1);
            return;
        }

        if (cli.help) {
            jc.usage();
            return;
        }

        System.exit(cli.run());
    }

    private int run() {
        try {
            if (!confDir.isDirectory()) {
                System.err.println("Config directory not found: " + confDir);
                return 1;
            }
            ConfigBootstrap.load(confDir);

            if (listTasks) {
                printTaskList(System.out);
                return 0;
            }

            if (taskClassName == null || input == null) {
                System.err.println("--task and --input are required (or use --list-tasks)");
                return 1;
            }

            List<IItem> items = StandaloneItemFactory.fromInput(input, recursive);
            RunResult result = TaskExecutor.run(taskClassName, items, confDir, extractChildrenTo);

            ResultWriter writer = "json".equalsIgnoreCase(outputFormat) ? new JsonResultWriter() : new StdoutResultWriter();
            if (outputFile != null) {
                try (PrintStream out = new PrintStream(outputFile, StandardCharsets.UTF_8)) {
                    writer.write(result, out);
                }
            } else {
                writer.write(result, System.out);
            }

            if (result.hasAnyError()) {
                return 1;
            }
            if (result.hasAnyUnsupported()) {
                return 2;
            }
            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e);
            return 1;
        }
    }

    private void printTaskList(PrintStream out) throws Exception {
        TaskInstallerConfig taskConfig = new TaskInstallerConfig();
        ConfigurationManager cm = ConfigurationManager.get();
        cm.addObject(taskConfig);
        cm.loadConfig(taskConfig);

        for (AbstractTask task : taskConfig.getNewTaskInstances()) {
            String className = task.getClass().getName();
            TaskCompatibility.Classification c = TaskCompatibility.classify(className);
            String detail = c.status() == TaskCompatibility.Status.COMPATIBLE ? "" : " -- " + c.reason();
            out.printf("%-65s %-18s%s%n", className, c.status(), detail);
        }
    }

    /** JCommander needs an explicit converter for {@link File}: it has no default one. */
    public static class FileConverter implements com.beust.jcommander.IStringConverter<File> {
        @Override
        public File convert(String value) {
            return new File(value);
        }
    }
}
