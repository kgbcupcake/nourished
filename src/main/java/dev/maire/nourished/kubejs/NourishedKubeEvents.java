package dev.maire.nourished.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.kubejs.events.NourishedFoodEatenEvent;
import dev.maire.nourished.kubejs.events.NourishedGutHealthChangedEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientChangedEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientCriticalEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientExcessEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientModifierEvent;
import dev.maire.nourished.kubejs.events.NourishedRawFoodPenaltyEvent;
import dev.maire.nourished.kubejs.events.NourishedSourceConsumedEvent;

import javax.annotation.Nullable;

@ApiStatus.Experimental
public final class NourishedKubeEvents {

    public static final String NUTRIENT_CHANGED_ID = "NourishedEvents.nutrientChanged";
    public static final String NUTRIENT_CRITICAL_ID = "NourishedEvents.nutrientCritical";
    public static final String NUTRIENT_EXCESS_ID = "NourishedEvents.nutrientExcess";
    public static final String SOURCE_CONSUMED_ID = "NourishedEvents.sourceConsumed";
    public static final String GUT_HEALTH_CHANGED_ID = "NourishedEvents.gutHealthChanged";
    public static final String RAW_FOOD_PENALTY_ID = "NourishedEvents.rawFoodPenalty";
    public static final String NUTRIENT_MODIFIER_ID = "NourishedEvents.nutrientModifier";
    public static final String FOOD_EATEN_ID = "NourishedEvents.foodEaten";

    public static final EventGroup GROUP = EventGroup.of("NourishedEvents");

    public static final EventHandler NUTRIENT_CHANGED =
            GROUP.server("nutrientChanged", () -> NourishedNutrientChangedEvent.class);
    public static final EventHandler NUTRIENT_CRITICAL =
            GROUP.server("nutrientCritical", () -> NourishedNutrientCriticalEvent.class);
    public static final EventHandler NUTRIENT_EXCESS =
            GROUP.server("nutrientExcess", () -> NourishedNutrientExcessEvent.class);
    public static final EventHandler SOURCE_CONSUMED =
            GROUP.server("sourceConsumed", () -> NourishedSourceConsumedEvent.class);
    public static final EventHandler GUT_HEALTH_CHANGED =
            GROUP.server("gutHealthChanged", () -> NourishedGutHealthChangedEvent.class);
    public static final EventHandler RAW_FOOD_PENALTY =
            GROUP.server("rawFoodPenalty", () -> NourishedRawFoodPenaltyEvent.class);
    public static final EventHandler NUTRIENT_MODIFIER =
            GROUP.server("nutrientModifier", () -> NourishedNutrientModifierEvent.class);
    public static final EventHandler FOOD_EATEN =
            GROUP.server("foodEaten", () -> NourishedFoodEatenEvent.class);

    private NourishedKubeEvents() {}

    @Nullable
    public static EventHandler handlerFor(String eventId) {
        return switch (eventId) {
            case NUTRIENT_CHANGED_ID -> NUTRIENT_CHANGED;
            case NUTRIENT_CRITICAL_ID -> NUTRIENT_CRITICAL;
            case NUTRIENT_EXCESS_ID -> NUTRIENT_EXCESS;
            case SOURCE_CONSUMED_ID -> SOURCE_CONSUMED;
            case GUT_HEALTH_CHANGED_ID -> GUT_HEALTH_CHANGED;
            case RAW_FOOD_PENALTY_ID -> RAW_FOOD_PENALTY;
            case NUTRIENT_MODIFIER_ID -> NUTRIENT_MODIFIER;
            case FOOD_EATEN_ID -> FOOD_EATEN;
            default -> null;
        };
    }
}
