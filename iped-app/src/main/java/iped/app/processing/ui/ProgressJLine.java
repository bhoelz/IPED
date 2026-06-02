package iped.app.processing.ui;

import iped.engine.core.Statistics;
import iped.engine.core.Worker;
import iped.utils.LocalizedFormat;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Status;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;

/**
 * Live TUI progress display for {@code --nogui} processing runs.
 *
 * Uses JLine3's {@link Status} to pin two status lines at the bottom of the
 * terminal while log messages scroll above. Falls back gracefully to
 * {@link ProgressConsole} when stdout is not a real TTY (see
 * {@link #tryCreate()}).
 */
public class ProgressJLine implements PropertyChangeListener {

    private static final String FILLED = "█";
    private static final String EMPTY  = "░";

    private final Terminal terminal;
    private final Status status;
    private final JLineAppender appender;
    private final NumberFormat numFmt = LocalizedFormat.getNumberInstance();

    private Worker[] workers;
    private boolean discoverEnded;
    private volatile String lastMessage = "";
    private long processingStart;

    private ProgressJLine(Terminal terminal, Status status, JLineAppender appender) {
        this.terminal = terminal;
        this.status   = status;
        this.appender = appender;
    }

    /**
     * Attempts to create a JLine3 TUI progress display.
     *
     * @return a new instance, or {@code null} if the current environment is not a
     *         real terminal (piped output, CI, dumb terminal).
     */
    public static ProgressJLine tryCreate() {
        try {
            Terminal t = TerminalBuilder.builder().system(true).dumb(false).build();
            if (Terminal.TYPE_DUMB.equals(t.getType()) || Terminal.TYPE_DUMB_COLOR.equals(t.getType())) {
                t.close();
                return null;
            }
            Status s = Status.getStatus(t, true);
            if (s == null) {
                t.close();
                return null;
            }
            JLineAppender appender = JLineAppender.install(t);
            return new ProgressJLine(t, s, appender);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (processingStart == 0) {
            processingStart = System.currentTimeMillis();
        }
        switch (evt.getPropertyName()) {
            case "workers":
                workers = (Worker[]) evt.getNewValue();
                break;
            case "discoverEnded":
                discoverEnded = true;
                redraw();
                break;
            case "update":
                redraw();
                break;
            case "mensagem":
                lastMessage = (String) evt.getNewValue();
                redraw();
                break;
            default:
                break;
        }
    }

    private void redraw() {
        Statistics s = Statistics.get();
        if (s == null) return;

        int totalVolumeMb  = (int) (s.getCaseData().getDiscoveredVolume() >>> 20);
        int totalItems     = s.getCaseData().getDiscoveredEvidences();
        int processedMb    = (int) (s.getVolume() >>> 20);
        int processedItems = s.getProcessed();

        long elapsedSecs = (System.currentTimeMillis() - processingStart) / 1000 + 1;
        long rateGBh     = processedMb * 3600L / (1024L * elapsedSecs);

        int percent = 0;
        if (discoverEnded && totalVolumeMb > 0) {
            percent = (int) Math.round(processedMb * 100.0 / totalVolumeMb);
        }

        String etaStr = "";
        if (discoverEnded && processedMb > 0 && totalVolumeMb > processedMb) {
            long secsToEnd = (long) (totalVolumeMb - processedMb)
                    * (System.currentTimeMillis() - processingStart)
                    / ((processedMb + 1) * 1000L);
            etaStr = "  ETA " + secsToEnd / 3600 + "h " + (secsToEnd / 60) % 60 + "m " + secsToEnd % 60 + "s";
        }

        int termWidth = Math.max(40, terminal.getWidth());
        int barWidth  = Math.max(10, termWidth - 52);
        int filled    = Math.min(barWidth, (percent * barWidth) / 100);

        AttributedStringBuilder line1 = new AttributedStringBuilder();
        line1.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
        line1.append("[").append(FILLED.repeat(filled)).append(EMPTY.repeat(barWidth - filled)).append("] ");
        line1.style(AttributedStyle.DEFAULT.bold());
        line1.append(numFmt.format(processedItems)).append("/").append(numFmt.format(totalItems));
        line1.style(AttributedStyle.DEFAULT);
        line1.append("  ").append(String.valueOf(percent)).append("%");
        line1.append("  ").append(String.valueOf(rateGBh)).append(" GB/h");
        line1.append(etaStr);

        AttributedStringBuilder line2 = new AttributedStringBuilder();
        line2.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        line2.append("Status: ").append(lastMessage.isEmpty() ? "Starting..." : lastMessage);
        int active = countActiveWorkers();
        if (active >= 0) {
            line2.style(AttributedStyle.DEFAULT);
            line2.append("   Workers: ").append(String.valueOf(active)).append(" active");
        }

        status.update(Arrays.asList(line1.toAttributedString(), line2.toAttributedString()));
    }

    private int countActiveWorkers() {
        if (workers == null) return -1;
        int count = 0;
        for (Worker w : workers) {
            if (w.isAlive() && !w.isWaiting()) count++;
        }
        return count;
    }

    /** Clears the status bar, restores the Log4j2 console appender, and closes the terminal. */
    public void close() {
        try {
            status.update(Collections.emptyList());
        } catch (Exception ignored) {}
        try {
            appender.uninstall();
        } catch (Exception ignored) {}
        try {
            terminal.close();
        } catch (Exception ignored) {}
    }
}
