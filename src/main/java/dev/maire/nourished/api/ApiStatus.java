package dev.maire.nourished.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for API stability annotations indicating the maturity level
 * of Nourished API surface elements.
 *
 * <p>Apply these annotations to types, methods, and fields to communicate
 * backwards-compatibility guarantees to downstream mod authors.</p>
 */
public final class ApiStatus {

    private ApiStatus() {}

    /**
     * Indicates a stable API element that will maintain backwards compatibility
     * across minor versions. Breaking changes require a major version bump.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Stable {}

    /**
     * Indicates an experimental API element that may change or be removed
     * in future versions without a major version bump. Use at your own risk.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Experimental {}

    /**
     * Indicates an internal API element not intended for use by external mods.
     * May change without notice in any release. Do not depend on these in production.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Internal {}
}
