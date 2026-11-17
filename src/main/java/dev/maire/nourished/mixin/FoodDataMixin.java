package dev.maire.nourished.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.maire.nourished.core.deficiency.DeficiencyPenalties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scales only food-driven natural regeneration by the player's deficiency tier. */
@Mixin(FoodData.class)
abstract class FoodDataMixin {

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void nourished$scaleRegen(Player player, float amount, Operation<Void> original) {
        original.call(player, amount * DeficiencyPenalties.regenMultiplier(player));
    }
}
