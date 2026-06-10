package dev.maire.nourished.core.handler;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tracking.TrackingAttachment;
import dev.marie.MariesLib.tracking.TrackingData;
import dev.marie.MariesLib.config.ModCompatRegistry;
import dev.marie.MariesLib.config.ModuleCache;
import dev.marie.MariesLib.util.MarieItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

@ApiStatus.Internal
public class NutritionEatingHandler {

    private static final int NUTRITION_ONLY_EAT_COOLDOWN_TICKS = 20;
    private static final HashMap<UUID, Long> LAST_NUTRITION_ONLY_EAT_GAME_TIME = new HashMap<>();
    /** ServerPlayer UUIDs waiting for {@link LivingEntityUseItemEvent.Finish} after {@link ServerPlayer#startUsingItem}. */
    private static final HashMap<UUID, PendingNutritionOnlyFinish> PENDING_NUTRITION_ONLY_FINISH = new HashMap<>();

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!ModuleCache.enableDecay || !ModuleCache.enableSourceApplication) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Long lastEat = LAST_NUTRITION_ONLY_EAT_GAME_TIME.get(player.getUUID());
        if (lastEat != null && now - lastEat < NUTRITION_ONLY_EAT_COOLDOWN_TICKS) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null || food.canAlwaysEat()) return;
        if (player.canEat(false)) return;
        if (shouldBlockNutritionOnlyAtFullHunger(stack, food)) {
            event.setCanceled(true);
            return;
        }
        ItemStack captured = stack.copy();
        InteractionHand hand = event.getHand();
        event.setCanceled(true);
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            server.execute(() -> performNutritionOnlyConsume(player, captured, hand));
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PendingNutritionOnlyFinish pending = PENDING_NUTRITION_ONLY_FINISH.get(player.getUUID());
        if (pending == null) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!pending.matches(stack)) {
            PENDING_NUTRITION_ONLY_FINISH.remove(player.getUUID());
            return;
        }
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) {
            PENDING_NUTRITION_ONLY_FINISH.remove(player.getUUID());
            return;
        }
        PENDING_NUTRITION_ONLY_FINISH.remove(player.getUUID());
        TrackingData diet = player.getData(TrackingAttachment.TRACKING.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        diet.tickTime(gameTimeMs);
        diet.tick();
        FoodNutrientPipeline.process(player, stack, diet, gameTimeMs);
        LAST_NUTRITION_ONLY_EAT_GAME_TIME.put(player.getUUID(), player.level().getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemUseStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PENDING_NUTRITION_ONLY_FINISH.remove(player.getUUID());
    }

    public static boolean isNutritionOnlyPipelinePending(ServerPlayer player) {
        return PENDING_NUTRITION_ONLY_FINISH.containsKey(player.getUUID());
    }

    public static boolean isNutritionOnlyPipelinePending(ServerPlayer player, ItemStack stack) {
        PendingNutritionOnlyFinish pending = PENDING_NUTRITION_ONLY_FINISH.get(player.getUUID());
        return pending != null && pending.matches(stack);
    }

    /**
     * When vanilla hunger is full, optionally block nutrition-only eating for heavy meals and light food.
     */
    private static boolean shouldBlockNutritionOnlyAtFullHunger(ItemStack stack, FoodProperties food) {
        if (ModuleCache.enableBlockHeavySources) {
            if (ModCompatRegistry.isLoaded("solonion")) {
                int threshold = ModCompatRegistry.getHeavySourceThreshold();
                if (food != null && food.nutrition() >= threshold) {
                    return true;
                }
            } else if (stack.is(MarieItemTags.heavySource())) {
                return true;
            }
        }
        return ModuleCache.enableBlockLightSource && stack.is(MarieItemTags.lightSource());
    }

    private static boolean performNutritionOnlyConsume(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        PENDING_NUTRITION_ONLY_FINISH.remove(player.getUUID());
        if (!ModuleCache.enableDecay || !ModuleCache.enableSourceApplication) {
            return false;
        }
        if (!player.isAlive()) {
            return false;
        }
        FoodProperties foodNow = stack.getItem().getFoodProperties(stack, player);
        if (foodNow == null || foodNow.canAlwaysEat()) {
            return false;
        }
        if (shouldBlockNutritionOnlyAtFullHunger(stack, foodNow)) {
            return false;
        }

        ItemStack live = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (live.isEmpty() || !ItemStack.isSameItemSameComponents(stack, live)) {
            return false;
        }

        if (player.isUsingItem()) {
            return false;
        }

        player.startUsingItem(hand);
        if (!player.isUsingItem()) {
            return false;
        }
        PENDING_NUTRITION_ONLY_FINISH.put(player.getUUID(), new PendingNutritionOnlyFinish(stack.copy()));
        return true;
    }

    private record PendingNutritionOnlyFinish(ItemStack stack) {
        private boolean matches(ItemStack candidate) {
            return !candidate.isEmpty() && ItemStack.isSameItemSameComponents(stack, candidate);
        }
    }
}
