package dev.maire.nourished.core.handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.util.NourishedItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@ApiStatus.Internal
public class NutritionEatingHandler {

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!ModuleCache.enableDecay || !ModuleCache.enableNutritionEating) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) {
            return;
        }
        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) {
            return;
        }
        if (ModuleCache.enableCalorieSaturationBlock && !food.canAlwaysEat()) {
            DietData diet = player.getData(DietAttachment.DIET.get());
            if (diet.calories >= diet.maxCalories && !player.canEat(false)) {
                event.setCanceled(true);
                return;
            }
        }
        if (food.canAlwaysEat()) {
            return;
        }
        if (player.canEat(false)) {
            return;
        }
        if (!qualifiesNutritionBypass(player, stack, food)) {
            return;
        }

        event.setCanceled(true);

        InteractionHand hand = event.getHand();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        server.execute(() -> performNutritionOnlyConsume(player, hand));
    }

    private static boolean qualifiesNutritionBypass(ServerPlayer player, ItemStack stack, FoodProperties food) {
        if (player.getFoodData().getFoodLevel() < 18) {
            return false;
        }
        if (stack.is(NourishedItemTags.MEAL)) {
            return false;
        }
        return food.nutrition() <= 2 || stack.is(NourishedItemTags.LIGHT_FOOD);
    }

    private static void performNutritionOnlyConsume(ServerPlayer player, InteractionHand hand) {
        if (!ModuleCache.enableDecay || !ModuleCache.enableNutritionEating) {
            return;
        }
        if (!player.isAlive()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }
        FoodProperties foodNow = stack.getItem().getFoodProperties(stack, player);
        if (foodNow == null || foodNow.canAlwaysEat() || player.canEat(false)) {
            return;
        }
        if (!qualifiesNutritionBypass(player, stack, foodNow)) {
            return;
        }

        ItemStack pipelineStack = stack.copy();

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.swing(InteractionHand.MAIN_HAND, true);

        SoundEvent eatSound = pipelineStack.getItem().getEatingSound();
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                eatSound,
                SoundSource.PLAYERS,
                0.5F + 0.5F * player.getRandom().nextFloat(),
                (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.2F + 1.0F);

        DietData diet = player.getData(DietAttachment.DIET.get());
        long gameTimeMs = player.level().getGameTime() * 50L;
        diet.tickTime(gameTimeMs);
        diet.tick();

        FoodNutrientPipeline.process(player, pipelineStack, diet, gameTimeMs);
    }
}
