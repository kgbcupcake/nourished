package dev.maire.nourished.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.maire.nourished.api.ApiStatus;

/**
 * KubeJS event declarations for Nourished (KubeJS 2101 event-group pattern).
 */
@ApiStatus.Internal
public interface NourishedKubeJSEvents {

    EventGroup GROUP = EventGroup.of("NourishedEvents");

    EventHandler nutrientChanged = GROUP.server("nutrientChanged", () -> NourishedKubeJSEventBridge.NutrientChangedKubeEvent.class);
    EventHandler nutrientCritical = GROUP.server("nutrientCritical", () -> NourishedKubeJSEventBridge.NutrientCriticalKubeEvent.class);
    EventHandler foodEaten = GROUP.server("foodEaten", () -> NourishedKubeJSEventBridge.FoodEatenKubeEvent.class);
    EventHandler nutrientModifier = GROUP.server("nutrientModifier", () -> NourishedKubeJSEventBridge.NutrientModifierKubeEvent.class);
}
