package dev.maire.nourished.compat.kubejs;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.NutrientModifierEvent;
import dev.maire.nourished.core.Nourished;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridges Nourished NeoForge events into KubeJS event IDs.
 */
@ApiStatus.Internal
public final class NourishedKubeJSEvents {

    public static final String NUTRIENT_CHANGED = "nourished.nutrient_changed";
    public static final String NUTRIENT_CRITICAL = "nourished.nutrient_critical";
    public static final String FOOD_EATEN = "nourished.food_eaten";
    public static final String NUTRIENT_MODIFIER = "nourished.nutrient_modifier";

    private static final EventGroup GROUP = EventGroup.of("NourishedEvents");
    private static final EventHandler NUTRIENT_CHANGED_HANDLER = GROUP.server(NUTRIENT_CHANGED, () -> NutrientChangedKubeEvent.class);
    private static final EventHandler NUTRIENT_CRITICAL_HANDLER = GROUP.server(NUTRIENT_CRITICAL, () -> NutrientCriticalKubeEvent.class);
    private static final EventHandler FOOD_EATEN_HANDLER = GROUP.server(FOOD_EATEN, () -> FoodEatenKubeEvent.class);
    private static final EventHandler NUTRIENT_MODIFIER_HANDLER = GROUP.server(NUTRIENT_MODIFIER, () -> NutrientModifierKubeEvent.class);

    private NourishedKubeJSEvents() {}

    public static void register(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(new NourishedKubeJSEvents());
    }

    @SubscribeEvent
    public void onNutrientChanged(NourishedEvents.NutrientChangedEvent event) {
        NUTRIENT_CHANGED_HANDLER.post(new NutrientChangedKubeEvent(event));
    }

    @SubscribeEvent
    public void onNutrientCritical(NourishedEvents.NutrientCriticalEvent event) {
        NUTRIENT_CRITICAL_HANDLER.post(new NutrientCriticalKubeEvent(event));
    }

    @SubscribeEvent
    public void onFoodEaten(NourishedEvents.FoodEatenEvent event) {
        FOOD_EATEN_HANDLER.post(new FoodEatenKubeEvent(event));
    }

    @SubscribeEvent
    public void onNutrientModifier(NutrientModifierEvent event) {
        NutrientModifierKubeEvent wrapped = new NutrientModifierKubeEvent(event);
        NUTRIENT_MODIFIER_HANDLER.post(wrapped);
        event.setCanceled(wrapped.cancelled);
        event.setAmount(wrapped.amount);
    }

    public static final class NutrientChangedKubeEvent implements KubeEvent {
        public Object player;
        public String nutrientKey;
        public float oldValue;
        public float newValue;

        public NutrientChangedKubeEvent() {}

        NutrientChangedKubeEvent(NourishedEvents.NutrientChangedEvent event) {
            this.player = event.getPlayer();
            this.nutrientKey = event.getNutrientKey();
            this.oldValue = event.getOldValue();
            this.newValue = event.getNewValue();
        }
    }

    public static final class NutrientCriticalKubeEvent implements KubeEvent {
        public Object player;
        public String nutrientKey;

        public NutrientCriticalKubeEvent() {}

        NutrientCriticalKubeEvent(NourishedEvents.NutrientCriticalEvent event) {
            this.player = event.getPlayer();
            this.nutrientKey = event.getNutrientKey();
        }
    }

    public static final class FoodEatenKubeEvent implements KubeEvent {
        public Object player;
        public String foodId;
        public String nutrientKey;
        public float amount;

        public FoodEatenKubeEvent() {}

        FoodEatenKubeEvent(NourishedEvents.FoodEatenEvent event) {
            this.player = event.getPlayer();
            this.foodId = event.getFoodId().toString();
            this.nutrientKey = event.getNutrientKey();
            this.amount = event.getAmount();
        }
    }

    public static final class NutrientModifierKubeEvent implements KubeEvent {
        public Object player;
        public String foodId;
        public String nutrientKey;
        public float amount;
        public boolean cancelled;

        public NutrientModifierKubeEvent() {}

        NutrientModifierKubeEvent(NutrientModifierEvent event) {
            this.player = event.getPlayer();
            this.foodId = event.getFoodId().toString();
            this.nutrientKey = event.getNutrientKey();
            this.amount = event.getAmount();
            this.cancelled = event.isCanceled();
        }
    }
}
