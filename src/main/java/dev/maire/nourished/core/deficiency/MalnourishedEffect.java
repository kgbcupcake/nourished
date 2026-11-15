package dev.maire.nourished.core.deficiency;

import dev.maire.nourished.core.Nourished;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Shows a player's deficiency level in the effect list (amplifier = tier - 1). The penalties come
 * from {@link DeficiencyPenalties}, so milk clearing the icon doesn't cure the deficiency.
 */
public final class MalnourishedEffect extends MobEffect {

    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Nourished.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> MALNOURISHED =
            MOB_EFFECTS.register("malnourished", MalnourishedEffect::new);

    private MalnourishedEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A7A45);
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
