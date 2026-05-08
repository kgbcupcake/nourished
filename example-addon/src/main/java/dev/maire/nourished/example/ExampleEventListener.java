package dev.maire.nourished.example;

import dev.maire.nourished.api.NourishedEvents;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Demonstrates consuming Nourished runtime events for gameplay messaging and logging.
 */
@EventBusSubscriber(modid = NourishedExampleAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ExampleEventListener {

    private ExampleEventListener() {}

    @SubscribeEvent
    public static void onNutrientCritical(NourishedEvents.NutrientCriticalEvent event) {
        if (!"fiber".equals(event.getNutrientKey())) {
            return;
        }
        event.getPlayer().sendSystemMessage(Component.literal("Warning: your Fiber level is critical!"));
    }

    @SubscribeEvent
    public static void onFoodEaten(NourishedEvents.FoodEatenEvent event) {
        System.out.println("[NourishedExampleAddon] Food eaten: " + event.getFoodId() + " nutrient=" + event.getNutrientKey() + " amount=" + event.getAmount());
    }
}
