package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.network.ModNetworking;
import dev.maire.nourished.modules.Stamina.Core.StaminaAttachment;
import dev.maire.nourished.modules.Stamina.Core.StaminaData;
import net.minecraft.server.level.ServerPlayer;

@ApiStatus.Internal
public final class StaminaDrainPipeline {

    private StaminaDrainPipeline() {}

    /**
     * Applies a stamina drain for the given action to the player.
     * Reads StaminaData, applies drain, writes back, syncs to client.
     * No-op if cost resolves to 0.
     */
    public static void apply(ServerPlayer player, StaminaActionType type) {
        float cost = StaminaCostResolver.resolve(type);
        if (cost <= 0f) return;

        StaminaData stamina = player.getData(StaminaAttachment.STAMINA.get());

        if (type.isPhysical()) {
            stamina.drainPhysical(cost);
        } else {
            stamina.drainMental(cost);
        }

        player.setData(StaminaAttachment.STAMINA.get(), stamina);
        ModNetworking.syncStamina(player, stamina);

        Nourished.LOGGER.debug(
                "[StaminaDrainPipeline] {} → {} drained {:.3f} ({} bar)",
                player.getName().getString(),
                type.name(),
                cost,
                type.isPhysical() ? "physical" : "mental"
        );
    }

    /**
     * Applies a raw stamina drain without resolving from config.
     * Used for custom costs (e.g. damage scaling).
     */
    public static void applyRaw(ServerPlayer player, StaminaActionType type, float amount) {
        if (amount <= 0f) return;

        StaminaData stamina = player.getData(StaminaAttachment.STAMINA.get());
        if (type.isPhysical()) {
            stamina.drainPhysical(amount);
        } else {
            stamina.drainMental(amount);
        }
        player.setData(StaminaAttachment.STAMINA.get(), stamina);
        ModNetworking.syncStamina(player, stamina);
    }
}
