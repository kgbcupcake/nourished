package dev.maire.nourished.mixin;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.config.ModuleCache;
import dev.maire.nourished.modules.Stamina.Action.StaminaActionType;
import dev.maire.nourished.modules.Stamina.Action.StaminaDrainPipeline;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
@ApiStatus.Internal
public abstract class PlayerJumpMixin {

    @SuppressWarnings("unused")
    @Inject(method = "jumpFromGround", at = @At("HEAD"), remap = false)
    private void nourished$onJump(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!ModuleCache.enableStamina) return;
        StaminaDrainPipeline.apply(serverPlayer, StaminaActionType.JUMP);
    }
}
