package dev.maire.nourished.core.context;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.ValueSourceTrigger;
import dev.maire.nourished.config.NourishedModuleCache;
import net.minecraft.server.level.ServerPlayer;

@ApiStatus.Internal
public final class NourishedSourceRules {

    private NourishedSourceRules() {}

    public static boolean isHeavyBlocked(ServerPlayer player, ValueSourceTrigger trigger) {
        if (trigger.type() != ValueSourceTrigger.TriggerType.ITEM_CONSUMED) {
            return false;
        }
        return (int) trigger.payload() >= NourishedModuleCache.heavySourcePropertyThreshold;
    }
}
