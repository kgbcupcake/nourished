package dev.maire.nourished.modules.activity_driven_nutrient.client;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side buffer of the local player's activity log, feeding {@link ActivityLogHudPanel}.
 * One fixed row per module id (see {@code ActivityDrivenNutrientRegistry}'s known module ids)
 * that updates its running count in place, rather than a scrolling feed of past entries.
 */
public final class ActivityLogClientBuffer {

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    private ActivityLogClientBuffer() {}

    public record Row(String moduleId, String description, int count, Instant timestamp) {}

    /**
     * Updates the row for {@code moduleId}: increments its count if the description hasn't
     * changed, otherwise starts a fresh row (a changed description means a config value like a
     * cost changed, so the old count no longer applies).
     */
    public static synchronized void append(String moduleId, String description, long timestampMillis) {
        Row existing = ROWS.get(moduleId);
        int count = (existing != null && existing.description().equals(description)) ? existing.count() + 1 : 1;
        ROWS.put(moduleId, new Row(moduleId, description, count, Instant.ofEpochMilli(timestampMillis)));
    }

    /** Current row for {@code moduleId}, or {@code null} if it hasn't fired yet this session. */
    public static synchronized Row get(String moduleId) {
        return ROWS.get(moduleId);
    }

    /** Number of module ids that have a row so far. */
    public static synchronized int size() {
        return ROWS.size();
    }

    /** Cleared on disconnect, same as the rest of Nourished's client-cached state. */
    public static synchronized void reset() {
        ROWS.clear();
    }
}
