package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedRawFoodPenaltyEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String itemId;
    public final String tier;
    private boolean cancelled;

    public NourishedRawFoodPenaltyEvent() {
        this.player = null;
        this.playerId = "";
        this.itemId = "";
        this.tier = "";
    }

    public NourishedRawFoodPenaltyEvent(String playerId, String itemId, String tier, Player player) {
        this.player = player;
        this.playerId = playerId;
        this.itemId = itemId;
        this.tier = tier;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
