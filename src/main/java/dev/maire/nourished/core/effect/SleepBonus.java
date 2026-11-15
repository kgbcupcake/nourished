package dev.maire.nourished.core.effect;

import dev.marie.framework.api.effects.SleepBonusEvaluator;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class SleepBonus implements SleepBonusEvaluator {

    public static final float THRESHOLD = 0.5f;
    public static final int DURATION_TICKS = 600;

    @Override
    public @Nullable MobEffectInstance evaluate(ServerPlayer player, Map<String, Float> values) {
        boolean any = false;
        for (NutrientRegistry.NutrientDef def : NutrientRegistry.getAll()) {
            if (!def.beneficial()) continue;
            Float level = values.get(def.key());
            if (level == null) continue;
            if (level < THRESHOLD) return null;
            any = true;
        }
        return any ? new MobEffectInstance(MobEffects.REGENERATION, DURATION_TICKS, 0) : null;
    }
}
