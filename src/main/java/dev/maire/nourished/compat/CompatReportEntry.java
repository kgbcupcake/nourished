package dev.maire.nourished.compat;

import org.jetbrains.annotations.Nullable;

public record CompatReportEntry(
        String modId,
        String displayName,
        CompatCategory category,
        boolean loaded,
        @Nullable String detectedVersion,
        ConflictLevel conflictLevel,
        boolean effectsDisabled,
        boolean decayDisabled
) {
    public static CompatReportEntry from(CompatEntry entry, ConflictBehavior mergedBehavior) {
        boolean effectsDisabled = entry.loaded() &&
                entry.conflictBehavior() != null &&
                entry.conflictBehavior().disableEffects();

        boolean decayDisabled = entry.loaded() &&
                entry.conflictBehavior() != null &&
                entry.conflictBehavior().disableDecay();

        return new CompatReportEntry(
                entry.modId(),
                entry.displayName(),
                entry.category(),
                entry.loaded(),
                entry.detectedVersion(),
                entry.resolvedConflictLevel(),
                effectsDisabled,
                decayDisabled
        );
    }
}
