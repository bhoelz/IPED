package iped.engine.config;

/**
 * Subset of the indexing configuration consumed by {@code iped-engine}
 * internals ({@code Manager}, {@code AppAnalyzer}, {@code ConfiguredFSDirectory},
 * {@code QueryBuilder}).
 *
 * <p>The concrete config class ({@code IndexTaskConfig}) is owned by
 * {@code iped-tasks-storage-index}, alongside the task that uses it. Engine
 * internals can't depend on that module without creating a cycle
 * ({@code iped-tasks-storage-index} already depends on {@code iped-engine}), so
 * they depend on this interface instead, declared here in
 * {@code iped-engine-core} which both sides already depend on. Looked up via
 * {@link ConfigurationManager#findObjectInstanceOf(Class)}, which matches by
 * {@code instanceof} rather than exact class — the registered configurable's
 * concrete class is never visible to the engine.
 */
public interface IndexSettings {

    boolean isUseNIOFSDirectory();

    boolean isForceMerge();

    int getCommitIntervalSeconds();

    int getMaxTokenLength();

    boolean isFilterNonLatinChars();

    boolean isConvertCharsToAscii();

    boolean isConvertCharsToLowerCase();

    int[] getExtraCharsToIndex();

}
