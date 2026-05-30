package dev.maire.nourished.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.api.NourishedEvents;
import dev.maire.nourished.api.NutrientModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridges Nourished NeoForge events into KubeJS handlers declared by {@link NourishedKubeJSEvents}.
 */
@ApiStatus.Internal
public final class NourishedKubeJSEventBridge {

    private static boolean registered;

    private NourishedKubeJSEventBridge() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.register(new NourishedKubeJSEventBridge());
    }

    @SubscribeEvent
    public void onNutrientChanged(NourishedEvents.NutrientChangedEvent event) {
        NourishedKubeJSEvents.nutrientChanged.post(new NutrientChangedKubeEvent(event));
    }

    @SubscribeEvent
    public void onNutrientCritical(NourishedEvents.NutrientCriticalEvent event) {
        NourishedKubeJSEvents.nutrientCritical.post(new NutrientCriticalKubeEvent(event));
    }

    @SubscribeEvent
    public void onFoodEaten(NourishedEvents.FoodEatenEvent event) {
        NourishedKubeJSEvents.foodEaten.post(new FoodEatenKubeEvent(event));
    }

    @SubscribeEvent
    public void onNutrientModifier(NutrientModifierEvent event) {
        NutrientModifierKubeEvent wrapped = new NutrientModifierKubeEvent(event);
        NourishedKubeJSEvents.nutrientModifier.post(wrapped);
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
