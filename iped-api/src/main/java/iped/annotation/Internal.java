package iped.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a publicly visible type or member as internal to the IPED engine.
 *
 * <p>Types annotated with {@code @Internal} are <em>not</em> part of the stable
 * scripting/plugin API and may change or be removed in any release without
 * advance notice. External plugin authors and script authors must not rely on
 * them.
 *
 * <p>Scripting SDK documentation generators should exclude types annotated
 * with this annotation from their output.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Internal {

    /**
     * Optional human-readable reason this type/member is internal.
     * Useful for engine developers to understand constraints.
     */
    String reason() default "";
}
