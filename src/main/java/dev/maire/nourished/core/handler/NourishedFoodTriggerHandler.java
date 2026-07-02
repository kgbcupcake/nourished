package dev.maire.nourished.core.handler;

import dev.marie.framework.api.ValueSourceTrigger;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieAPI;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.tracking.TrackingAttachment;
import dev.marie.framework.util.MarieRegistryUtils;
import dev.maire.nourished.core.NourishedKubeIntegration;
import dev.maire.nourished.core.nutrition.FoodNutritionRegistry;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Nourished-owned food trigger handler.
 * Fires MarieLib's value pipeline when a player eats food.
 * This logic lives in Nourished, not in MarieLib.
 */
@ApiStatus.Internal
public final class NourishedFoodTriggerHandler {

    private static final int SOURCE_ONLY_EAT_COOLDOWN_TICKS = 20;
    private static final HashMap<UUID, Long> LAST_SOURCE_ONLY_EAT = new HashMap<>();
    private static final HashMap<UUID, PendingFinish> PENDING_FINISH = new HashMap<>();

    private NourishedFoodTriggerHandler() {}

    public static void register(IEventBus eventBus) {
        var instance = new Listener();
        eventBus.addListener(EventPriority.HIGH, instance::onItemUseFinishHigh);
        eventBus.addListener(instance::onItemUseFinish);
        eventBus.addListener(EventPriority.LOWEST, instance::onItemUseStop);
        eventBus.addListener(instance::onRightClick);
    }

    private static class Listener {

        public void onItemUseFinishHigh(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (!FeatureFlagCache.enableSourceApplication()) return;
            if (!TrackingAttachment.isRegistered()) return;
            ItemStack stack = event.getItem();
            if (stack == null || stack.isEmpty()) {
                return;
            }
            if (PENDING_FINISH.containsKey(player.getUUID())) return;
            FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, player);
            if (food == null) return;
            fireSourceTriggerAndNotifyFoodEaten(player, stack, food);
        }

        public void onRightClick(PlayerInteractEvent.RightClickItem event) {
            if (!FeatureFlagCache.enableDecay() || !FeatureFlagCache.enableSourceApplication()) return;
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            long now = player.level().getGameTime();
            Long last = LAST_SOURCE_ONLY_EAT.get(player.getUUID());
            if (last != null && now - last < SOURCE_ONLY_EAT_COOLDOWN_TICKS) return;
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;
            FoodProperties food = stack.getItem().getFoodProperties(stack, player);
            if (food == null || food.canAlwaysEat()) return;
            if (player.canEat(false)) return;
            ItemStack captured = stack.copy();
            InteractionHand hand = event.getHand();
            event.setCanceled(true);
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                server.execute(() -> {
                    player.startUsingItem(hand);
                    PENDING_FINISH.put(player.getUUID(),
                        new PendingFinish(captured.getItem(), captured.getCount()));
                });
            }
        }

        public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            PendingFinish pending = PENDING_FINISH.get(player.getUUID());
            if (pending == null) return;
            ItemStack stack = event.getItem();
            if (stack == null || stack.isEmpty()) {
                PENDING_FINISH.remove(player.getUUID());
                return;
            }
            if (!pending.matches(stack)) {
                PENDING_FINISH.remove(player.getUUID());
                return;
            }
            PENDING_FINISH.remove(player.getUUID());
            if (!TrackingAttachment.isRegistered()) return;
            FoodProperties food = FoodNutritionRegistry.foodPropertiesForNutrition(stack, player);
            if (food == null) return;
            fireSourceTriggerAndNotifyFoodEaten(player, stack, food);
            LAST_SOURCE_ONLY_EAT.put(player.getUUID(), player.level().getGameTime());
        }

        public void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            PENDING_FINISH.remove(player.getUUID());
        }
    }

    private static void fireSourceTriggerAndNotifyFoodEaten(
            ServerPlayer player,
            ItemStack stack,
            FoodProperties food
    ) {
        ResourceLocation itemId = MarieRegistryUtils.itemKey(stack);
        Map<String, Float> before = snapshotNutrientLevels(player);
        MarieAPI.fireSourceTrigger(
                player,
                ValueSourceTrigger.itemConsumed(itemId, food.nutrition()),
                stack);
        Map<String, Float> deltas = computeNutrientDeltas(before, snapshotNutrientLevels(player));
        NourishedKubeIntegration.fireFoodEaten(player, itemId.toString(), deltas);
    }

    private static Map<String, Float> snapshotNutrientLevels(ServerPlayer player) {
        Map<String, Float> levels = new LinkedHashMap<>();
        for (NutrientRegistry.NutrientDef nutrient : NutrientRegistry.getAll()) {
            levels.put(nutrient.key(), nutrientLevel(player, nutrient.key()));
        }
        return levels;
    }

    private static Map<String, Float> computeNutrientDeltas(
            Map<String, Float> before,
            Map<String, Float> after
    ) {
        Map<String, Float> deltas = new LinkedHashMap<>();
        for (NutrientRegistry.NutrientDef nutrient : NutrientRegistry.getAll()) {
            String key = nutrient.key();
            float delta = after.getOrDefault(key, 0f) - before.getOrDefault(key, 0f);
            if (delta != 0f) {
                deltas.put(key, delta);
            }
        }
        return deltas;
    }

    private static float nutrientLevel(ServerPlayer player, String nutrientKey) {
        float level = MarieAPI.getValueLevel(player, nutrientKey);
        return level < 0f ? 0f : level;
    }

    private record PendingFinish(net.minecraft.world.item.Item item, int count) {
        boolean matches(ItemStack stack) {
            return stack.getItem() == item && stack.getCount() == count;
        }
    }
}
