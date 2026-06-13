package iped.datasource.spi;

import iped.data.IItem;
import iped.datasource.IDataSource;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * SPI for pluggable evidence datasource readers.
 *
 * <p>Datasource reader implementations (e.g., Sleuth Kit, UFED, AD1) register
 * via {@link java.util.ServiceLoader} by placing their fully-qualified class
 * name in
 * {@code META-INF/services/iped.datasource.spi.IDataSourceReader}.
 *
 * <p>The engine discovers all registered readers at startup and delegates to
 * the first reader that {@link #accepts(File) accepts} a given evidence file,
 * eliminating the need for hardcoded datasource wiring in engine code.
 *
 * <p>Implementations must have a public no-arg constructor and must be
 * thread-safe if the same instance is shared across cases.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Engine calls {@link #accepts(File)} to find the right reader.</li>
 *   <li>Engine calls {@link #open(IDataSource, File)} to initialise reading.</li>
 *   <li>Engine iterates items via {@link #iterator()}.</li>
 *   <li>Engine calls {@link #close()} when done.</li>
 * </ol>
 */
public interface IDataSourceReader extends Closeable {

    /**
     * Returns {@code true} if this reader can open the given evidence file.
     *
     * <p>The engine calls this on every registered reader in service-discovery
     * order and uses the first one that returns {@code true}.
     *
     * @param evidenceFile path to the evidence file or directory
     * @return {@code true} if this reader handles the given file type
     */
    boolean accepts(File evidenceFile);

    /**
     * Opens the datasource for reading.
     *
     * @param dataSource metadata about the datasource (name, UUID)
     * @param evidenceFile path to the evidence file or directory
     * @throws IOException if the datasource cannot be opened
     */
    void open(IDataSource dataSource, File evidenceFile) throws IOException;

    /**
     * Returns an iterator over the items in this datasource.
     *
     * <p>{@link #open(IDataSource, File)} must be called before this method.
     * The iterator may be lazy (streaming) and need not support {@link Iterator#remove()}.
     *
     * @return iterator over discovered items; never {@code null}
     */
    Iterator<IItem> iterator();

    /**
     * Human-readable name for this reader, used in log messages and diagnostics.
     *
     * @return reader name, e.g. {@code "Sleuth Kit"} or {@code "UFED"}
     */
    String getName();

    /**
     * Releases all resources held by this reader.  Called after all items
     * have been consumed or if an error occurs during reading.
     *
     * @throws IOException if resources cannot be released cleanly
     */
    @Override
    void close() throws IOException;
}
