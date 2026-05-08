package dev.maire.nourished.compat;

import com.google.gson.annotations.SerializedName;
import dev.maire.nourished.api.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public record CompatEntry(
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

        int priority,

        // Runtime-resolved fields (not from JSON)
        boolean loaded,
        @Nullable String detectedVersion,
        ConflictLevel resolvedConflictLevel
) {
    public static CompatEntry withRuntimeData(
            CompatEntry base,
            boolean loaded,
            @Nullable String detectedVersion,
            ConflictLevel resolvedConflictLevel
    ) {
        return new CompatEntry(
                base.modId(),
                base.displayName(),
                base.category(),
                base.namespaces(),
                base.providesFoodTags(),
                base.handlesOwnNutrition(),
                base.versionRanges(),
                base.conflictBehavior(),
                base.softCompat(),
                base.priority(),
                loaded,
                detectedVersion,
                resolvedConflictLevel
        );
    }

    public static CompatEntry createUnknown(String modId, List<String> namespaces) {
        return new CompatEntry(
                modId,
                modId,
                CompatCategory.UNKNOWN,
                namespaces,
                false,
                false,
                Map.of(),
                null,
                false,
                0,
                true,
                null,
                ConflictLevel.NONE
        );
    }
}
