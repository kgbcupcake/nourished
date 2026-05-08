package dev.maire.nourished.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Structured deprecation marker for public Nourished API elements.
 *
 * <p>Use this annotation instead of bare {@link Deprecated} so addon
 * developers always have migration context.</p>
 */
@Documented
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@ApiStatus.Stable
public @interface NourishedDeprecated {

    /**
     * API version in which this element was deprecated.
     */
    String since();

    /**
     * Suggested replacement API symbol or usage pattern.
     */
    String replacement();

    /**
     * Rationale for the deprecation and migration guidance.
     */
    String reason();
}
