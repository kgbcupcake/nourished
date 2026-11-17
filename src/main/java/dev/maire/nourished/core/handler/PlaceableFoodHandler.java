package dev.maire.nourished.core.handler;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

/**
 * Gives nutrition for foods eaten straight off a placed block (vanilla cake, modded pies).
 * There's no item-use event for these, so we compare hunger before and after the click.
 */
@ApiStatus.Internal
public final class PlaceableFoodHandler {

    private static final HashMap<UUID, Snapshot> PENDING = new HashMap<>();

    private PlaceableFoodHandler() {}

    public static void register(IEventBus eventBus) {
        eventBus.addListener(EventPriority.LOWEST, PlaceableFoodHandler::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getUseBlock() == TriState.FALSE) return;
        if (!FeatureFlagCache.enableSourceApplication()) return;
        if (!TrackingAttachment.isRegistered()) return;

        ItemStack blockItem = new ItemStack(event.getLevel().getBlockState(event.getPos()).getBlock().asItem());
        if (blockItem.isEmpty()) return;
        FoodProperties base = FoodNutritionRegistry.foodPropertiesForNutrition(blockItem, player);
        if (base == null) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        FoodData food = player.getFoodData();
        Snapshot snapshot = new Snapshot(blockItem, base, food.getFoodLevel(), food.getSaturationLevel());
        // the offhand retry is a second event; only the latest snapshot gets to fire
        PENDING.put(player.getUUID(), snapshot);
        // tell() always queues; execute() would run inline if this fired outside a packet task (fake players)
        server.tell(new TickTask(server.getTickCount(), () -> resolve(player, snapshot)));
    }

    private static void resolve(ServerPlayer player, Snapshot snapshot) {
        if (!PENDING.remove(player.getUUID(), snapshot)) return;
        if (player.isRemoved()) return;
        FoodData food = player.getFoodData();
        int hungerGain = food.getFoodLevel() - snapshot.foodLevel;
        float saturationGain = food.getSaturationLevel() - snapshot.saturation;
        if (hungerGain <= 0 && saturationGain <= 0f) return;

        NourishedFoodTriggerHandler.fireSourceTriggerAndNotifyFoodEaten(
                player, snapshot.stack, propertiesFor(snapshot.base, hungerGain, saturationGain));
    }

    // cake-style items have no food component, so build one from what the bite actually gave
    private static FoodProperties propertiesFor(FoodProperties base, int hungerGain, float saturationGain) {
        if (base.nutrition() > 0) return base;
        int nutrition = Math.max(0, hungerGain);
        float modifier = nutrition > 0 ? Math.max(0f, saturationGain) / (2f * nutrition) : 0f;
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(modifier).build();
    }

    private record Snapshot(ItemStack stack, FoodProperties base, int foodLevel, float saturation) {}
}
