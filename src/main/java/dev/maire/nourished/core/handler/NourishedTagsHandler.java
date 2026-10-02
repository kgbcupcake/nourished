package dev.maire.nourished.core.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPIState;
import dev.marie.framework.config.FeatureFlagCache;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@ApiStatus.Internal
public final class NourishedTagsHandler {

    private NourishedTagsHandler() {}

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("TagsUpdatedEvent fired");
        }
        // No clearExternalClassifications() here: NourishedDatapackCallbacks.onApplyBegin() already
        // clears once per reload, and TagsUpdatedEvent fires *after* that apply. Clearing again
        // wiped every datapack source_classifications/*.json entry (e.g. addon hydration mappings),
        // leaving only the tag-derived values. Tag scores are merged on top of them instead.
        try (MarieAPIState.DatapackReloadScope scope = MarieAPIState.openForDatapackReload()) {
            NutrientRegistry.registerClassificationsFromTags();
        }
        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("Classifications registered");
        }
    }
}
