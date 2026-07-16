package iped.datasource;

/** Declares the consistency guarantee provided by an additional store. */
public enum AdditionalStoreConsistencyPolicy {
    /** Writes become visible atomically after an explicit commit. */
    COMMIT_ATOMIC,
    /** Reads may lag writes and are eventually convergent. */
    EVENTUAL,
    /** The connector does not provide a cross-record consistency guarantee. */
    NONE
}
