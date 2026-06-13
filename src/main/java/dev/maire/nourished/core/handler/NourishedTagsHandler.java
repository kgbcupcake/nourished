package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.config.FeatureFlagCache;
import dev.marie.MariesLib.runtime.SourceRegistry;
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
        SourceRegistry.clearExternalClassifications();
        try (MarieAPIState.DatapackReloadScope scope = MarieAPIState.openForDatapackReload()) {
            NutrientRegistry.registerClassificationsFromTags();
        }
        if (FeatureFlagCache.enableDebugLogging()) {
            Nourished.LOGGER.debug("Classifications registered");
        }
    }
}
