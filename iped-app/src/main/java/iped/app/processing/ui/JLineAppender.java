package iped.app.processing.ui;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jline.terminal.Terminal;

/**
 * Log4j2 appender that routes log output through the JLine3 terminal writer so
 * the scroll region managed by {@link org.jline.utils.Status} is never
 * corrupted by direct writes to System.out.
 *
 * Install once before activating the TUI; uninstall on shutdown.
 */
public class JLineAppender extends AbstractAppender {

    private static final String APPENDER_NAME = "JLineAppender";

    private final PrintWriter writer;
    private final List<String> suspendedConsoleAppenders = new ArrayList<>();

    private JLineAppender(Terminal terminal, Layout<? extends Serializable> layout) {
        super(APPENDER_NAME, (Filter) null, layout, true, Property.EMPTY_ARRAY);
        this.writer = terminal.writer();
    }

    /**
     * Creates, starts, and registers this appender with the root logger.
     * Existing Console appenders are removed so only JLine touches the terminal.
     */
    public static JLineAppender install(Terminal terminal) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d{yyyy-MM-dd HH:mm:ss}\t[%level]\t[%logger{3}]\t\t\t%msg%n")
                .withConfiguration(config)
                .build();

        JLineAppender appender = new JLineAppender(terminal, layout);
        appender.start();

        LoggerConfig root = config.getRootLogger();

        // Suspend console appenders to avoid double-writing to the terminal
        for (Map.Entry<String, Appender> entry : new ArrayList<>(root.getAppenders().entrySet())) {
            if (entry.getValue().getClass().getSimpleName().toLowerCase().contains("console")) {
                root.removeAppender(entry.getKey());
                appender.suspendedConsoleAppenders.add(entry.getKey());
            }
        }

        config.addAppender(appender);
        root.addAppender(appender, root.getLevel(), null);
        ctx.updateLoggers();

        return appender;
    }

    /** Removes this appender and restores the previously suspended Console appenders. */
    public void uninstall() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig root = config.getRootLogger();

        root.removeAppender(APPENDER_NAME);

        for (String name : suspendedConsoleAppenders) {
            Appender ca = config.getAppender(name);
            if (ca != null) {
                root.addAppender(ca, root.getLevel(), null);
            }
        }

        stop();
        ctx.updateLoggers();
    }

    @Override
    public void append(LogEvent event) {
        writer.print(new String(getLayout().toByteArray(event)));
        writer.flush();
    }
}
