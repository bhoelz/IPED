package iped.engine.core;

/**
 * Thread-local holder for CaseContext. Allows each worker thread to access its
 * associated case context without needing to pass it through every method call.
 *
 * Usage:
 * - In worker thread: CaseContextThreadLocal.set(caseContext)
 * - In task/processor: CaseContext ctx = CaseContextThreadLocal.get()
 * - On thread cleanup: CaseContextThreadLocal.clear()
 */
public class CaseContextThreadLocal {

    private static final ThreadLocal<CaseContext> threadLocal = new ThreadLocal<>();

    /**
     * Set the current thread's case context.
     *
     * @param context the case context for this thread
     */
    public static void set(CaseContext context) {
        threadLocal.set(context);
    }

    /**
     * Get the current thread's case context.
     *
     * @return the case context, or null if not set
     */
    public static CaseContext get() {
        return threadLocal.get();
    }

    /**
     * Clear the current thread's case context. Should be called in finally blocks
     * to prevent context leaks.
     */
    public static void clear() {
        threadLocal.remove();
    }

    /**
     * Check if a context is set for the current thread.
     *
     * @return true if context is set, false otherwise
     */
    public static boolean isSet() {
        return threadLocal.get() != null;
    }
}
