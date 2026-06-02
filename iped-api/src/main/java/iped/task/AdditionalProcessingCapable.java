package iped.task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@code AbstractTask} subclass as safe to run post-indexing on a
 * user-selected subset of items.
 *
 * <p>Tasks annotated with this annotation must satisfy the following
 * contract:</p>
 * <ul>
 *   <li>They MUST NOT attempt to create new child items (i.e. they must not
 *       call {@code worker.processNewItem()}).</li>
 *   <li>They MUST be idempotent: running the same task twice on the same item
 *       must produce the same result (the previous result will be overwritten
 *       in the additional index).</li>
 *   <li>They MUST be instantiable via a public no-arg constructor.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdditionalProcessingCapable {

    /** Human-readable name shown in the GUI task-selection dialog. */
    String displayName();

    /** Short description of what the task does. */
    String description() default "";

    /**
     * {@code true} if the task reads the binary content of the file
     * (e.g. OCR, transcription).  Used by the GUI to warn the user when
     * source media is unavailable.
     */
    boolean requiresFileContent() default false;
}
