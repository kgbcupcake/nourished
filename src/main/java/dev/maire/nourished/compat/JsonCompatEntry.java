package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;
import dev.maire.nourished.api.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * JSON-serializable version of CompatEntry without runtime fields.
 * Used for parsing JSON files, then converted to CompatEntry with runtime data.
 */
@ApiStatus.Internal
public record JsonCompatEntry(
        @SerializedName("mod_id")
        String modId,

        @SerializedName("display_name")
        String displayName,

        CompatCategory category,

        List<String> namespaces,

        @SerializedName("provides_food_tags")
        boolean providesFoodTags,

        @SerializedName("handles_own_nutrition")
        boolean handlesOwnNutrition,

        @SerializedName("version_ranges")
        Map<String, ConflictLevel> versionRanges,

        @SerializedName("conflict_behavior")
        @Nullable ConflictBehavior conflictBehavior,

        @SerializedName("soft_compat")
        boolean softCompat,

        int priority
) {
    public CompatEntry toCompatEntry(boolean loaded, @Nullable String detectedVersion, ConflictLevel resolvedConflictLevel) {
        return new CompatEntry(
                modId,
                displayName,
                category != null ? category : CompatCategory.UNKNOWN,
                namespaces != null ? namespaces : List.of(),
                providesFoodTags,
                handlesOwnNutrition,
                versionRanges != null ? versionRanges : Map.of(),
                conflictBehavior,
                softCompat,
                priority,
                loaded,
                detectedVersion,
                resolvedConflictLevel
        );
    }

    public CompatEntry toCompatEntry() {
        return toCompatEntry(false, null, ConflictLevel.NONE);
    }
}
