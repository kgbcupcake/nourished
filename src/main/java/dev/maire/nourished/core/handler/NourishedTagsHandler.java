package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieAPIState;
import dev.marie.MariesLib.runtime.SourceRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@ApiStatus.Internal
public final class NourishedTagsHandler {

    private NourishedTagsHandler() {}

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        SourceRegistry.clearExternalClassifications();
        try (MarieAPIState.DatapackReloadScope scope = MarieAPIState.openForDatapackReload()) {
            NutrientRegistry.registerClassificationsFromTags();
        }
    }
}
