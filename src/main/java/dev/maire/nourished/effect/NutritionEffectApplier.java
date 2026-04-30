package dev.maire.nourished.effect;

import dev.maire.nourished.attachment.NutritionData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Objects;

public final class NutritionEffectApplier {

    private static final float LOW_THRESHOLD = 0.25f;
    private static final float HIGH_THRESHOLD = 0.75f;
    private static final float BALANCE_TOLERANCE = 0.15f;
    private static final int EFFECT_DURATION_TICKS = 140;

    private NutritionEffectApplier() {}

    public static void apply(ServerPlayer player, NutritionData data) {
        applyEffect(player, MobEffects.DIG_SLOWDOWN, data.protein < LOW_THRESHOLD, 0);
        applyEffect(player, MobEffects.WEAKNESS, data.carbs < LOW_THRESHOLD, 0);
        applyEffect(player, MobEffects.UNLUCK, data.vitamins < LOW_THRESHOLD, 0);
        applyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, data.hydration < LOW_THRESHOLD, 0);

        boolean allMacrosHigh = data.protein > HIGH_THRESHOLD
                && data.carbs > HIGH_THRESHOLD
                && data.fats > HIGH_THRESHOLD;
        boolean balanced = isBalanced(data);

        applyEffect(player, MobEffects.HEALTH_BOOST, allMacrosHigh, 0);
        applyEffect(player, MobEffects.REGENERATION, allMacrosHigh && balanced, 0);
    }

    private static boolean isBalanced(NutritionData data) {
        float max = Math.max(data.protein, Math.max(data.carbs, data.fats));
        float min = Math.min(data.protein, Math.min(data.carbs, data.fats));
        return (max - min) <= BALANCE_TOLERANCE;
    }

    private static void applyEffect(ServerPlayer player, Holder<MobEffect> effect, boolean enabled, int amplifier) {
        Holder<MobEffect> safeEffect = Objects.requireNonNull(effect);
        if (enabled) {
            player.addEffect(new MobEffectInstance(safeEffect, EFFECT_DURATION_TICKS, amplifier, true, false, true));
        } else {
            player.removeEffect(safeEffect);
        }
    }
}
