package dev.maire.nourished.modules.Stamina.Handler;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.modules.Stamina.Action.StaminaActionType;
import dev.maire.nourished.modules.Stamina.Action.StaminaDrainPipeline;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@ApiStatus.Internal
public class StaminaWorldHandler {

    @SubscribeEvent
    public void onMine(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;
        StaminaDrainPipeline.apply(player, StaminaActionType.MINE);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModuleCache.enableStamina) return;
        StaminaDrainPipeline.apply(player, StaminaActionType.PLACE);
    }
}
