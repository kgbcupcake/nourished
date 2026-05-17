package dev.maire.nourished.modules.Stamina.Handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.core.diet.DietAttachment;
import dev.maire.nourished.core.diet.DietData;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthAttachment;
import dev.maire.nourished.modules.RawFood.Gut.GutHealthData;
import dev.maire.nourished.modules.Stamina.Core.StaminaAttachment;
import dev.maire.nourished.modules.Stamina.Core.StaminaConfig;
import dev.maire.nourished.modules.Stamina.Core.StaminaData;
import dev.maire.nourished.modules.Stamina.Nutrition.StaminaNutritionProfile;
import dev.maire.nourished.modules.Stamina.Nutrition.StaminaNutritionResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@ApiStatus.Internal
public class StaminaTickHandler {

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!ModuleCache.enableStamina) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int interval = StaminaConfig.regenDelay();
        if (interval <= 0) return;
        if (player.level().getGameTime() % interval != 0) return;

        StaminaData stamina = player.getData(StaminaAttachment.STAMINA.get());
        DietData diet = player.getData(DietAttachment.DIET.get());
        GutHealthData gut = player.getData(GutHealthAttachment.GUT.get());

        StaminaNutritionProfile profile = StaminaNutritionResolver.resolve(diet, gut);

        stamina.applyNutritionModifiers(diet.nutrients, gut.getGutHealth());

        float physicalRegen = StaminaConfig.basePhysicalRegen() * profile.regenModifier();
        float mentalRegen = StaminaConfig.baseMentalRegen() * profile.regenModifier();
        if (isResting(player)) {
            physicalRegen *= StaminaConfig.regenRestMultiplier();
            mentalRegen *= StaminaConfig.regenRestMultiplier();
        }
        stamina.regenPhysical(physicalRegen);
        stamina.regenMental(mentalRegen);

        float effectiveFatigueBuild = StaminaConfig.fatigueBuildRate()
                * (1.0f - profile.fatigueResistance());

        boolean physicalFatigued = stamina.effectivePhysicalStamina() / stamina.effectivePhysicalMax()
                < StaminaConfig.fatigueThreshold();
        boolean mentalFatigued = stamina.effectiveMentalStamina() / stamina.effectiveMentalMax()
                < StaminaConfig.fatigueThreshold();

        if (physicalFatigued) {
            stamina.applyFatigue(true, effectiveFatigueBuild);
        } else {
            stamina.recoverFatigue(true, StaminaConfig.fatigueDecayRate());
        }

        if (mentalFatigued) {
            stamina.applyFatigue(false, effectiveFatigueBuild);
        } else {
            stamina.recoverFatigue(false, StaminaConfig.fatigueDecayRate());
        }

        stamina.decayPhysicalBonus(StaminaConfig.bonusDecayRate());
        stamina.decayMentalBonus(StaminaConfig.bonusDecayRate());

        player.setData(StaminaAttachment.STAMINA.get(), stamina);
        ModNetworking.syncStamina(player, stamina);
    }

    private static boolean isResting(ServerPlayer player) {
        Vec3 vel = player.getDeltaMovement();
        return Math.abs(vel.x) < 0.01 && Math.abs(vel.z) < 0.01
                && player.onGround();
    }
}
