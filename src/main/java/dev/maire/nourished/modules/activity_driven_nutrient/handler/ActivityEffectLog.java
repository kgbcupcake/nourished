package dev.maire.nourished.modules.activity_driven_nutrient.handler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Bounded, in-memory log of "effect applied" events for the activity-driven nutrient modules —
 * feeds the (not yet built) debug HUD overlay. Newest entries are appended at the tail; once at
 * capacity the oldest entry is dropped. Not persisted — this is a live debug view, not history.
 */
public final class ActivityEffectLog {

    private static final int CAPACITY = 50;
    private static final Deque<Entry> ENTRIES = new ArrayDeque<>(CAPACITY);

    private ActivityEffectLog() {}

    public record Entry(String moduleId, String playerName, String description, Instant timestamp) {}

    public static synchronized Entry record(String moduleId, String playerName, String description) {
        if (ENTRIES.size() == CAPACITY) {
            ENTRIES.removeFirst();
        }
        Entry entry = new Entry(moduleId, playerName, description, Instant.now());
        ENTRIES.addLast(entry);
        return entry;
    }

    /** Newest-last snapshot of currently buffered entries. */
    public static synchronized List<Entry> snapshot() {
        return List.copyOf(ENTRIES);
    }

    /** Shared "Label: +0.0000 all nutrients" formatting for module descriptions. */
    public static String formatDelta(String label, float delta) {
        return String.format("%s: %+.4f all nutrients", label, delta);
    }
}
