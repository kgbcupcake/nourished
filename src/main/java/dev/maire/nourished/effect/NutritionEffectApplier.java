package dev.maire.nourished.effect;

import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.NutrientRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class NutritionEffectApplier {

    private NutritionEffectApplier() {}

    public static void apply(ServerPlayer player, DietData data) {
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            if (!def.enabled()) continue;
            boolean shouldApply = switch (def.trigger()) {
                case "below" -> data.nutrients.getOrDefault(def.nutrient(), 0f) < def.threshold();
                case "all_above" -> NutrientRegistry.getKeys().stream()
                        .allMatch(k -> data.nutrients.getOrDefault(k, 0f) > def.threshold());
                default -> false;
            };
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceLocation.parse(def.effect()))
                    .orElse(null);
            if (effect == null) continue;
            applyEffect(player, effect, shouldApply, def.amplifier(), def.durationTicks());
        }
    }

    private static void applyEffect(ServerPlayer player, Holder<MobEffect> effect, boolean enabled, int amplifier, int durationTicks) {
        if (enabled) {
            player.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, true, false, true));
        } else {
            player.removeEffect(effect);
        }
    }
}
