package dev.maire.nourished.core.nutrition;

/**
 * Identifies the final authoritative stage that produced the resolved nutrient map for an item
 * in the full nutrition resolution pipeline.
 *
 * <p>This enum tracks the high-level pipeline outcome, distinct from {@link RuntimeCascadeStage}
 * which tracks the internal inference stages within {@link RuntimeFoodResolver}.</p>
 *
 * <p>Stage assignment after merging all sources:</p>
 * <ul>
 *   <li>{@link #TAG_MATCH} — Compat-filtered tags non-empty, resolver empty (no blend).</li>
 *   <li>{@link #SCANNER_CLASSIFIED} — Tags empty, resolver empty, external (API/scanner) non-empty.</li>
 *   <li>{@link #RUNTIME_RESOLVER} — Tags empty, resolver non-empty, external empty.</li>
 *   <li>{@link #BLENDED} — Tags and resolver both non-empty (blend applied).</li>
 *   <li>{@link #UNCLASSIFIED} — Final merged bar map is empty.</li>
 * </ul>
 */
public enum ResolutionStage {
    TAG_MATCH,
    SCANNER_CLASSIFIED,
    RUNTIME_RESOLVER,
    BLENDED,
    UNCLASSIFIED
}
