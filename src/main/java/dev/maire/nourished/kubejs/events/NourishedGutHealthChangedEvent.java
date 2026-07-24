package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.world.entity.player.Player;

@ApiStatus.Experimental
public class NourishedGutHealthChangedEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final float oldValue;
    public final float newValue;
    public final String cause;

    public NourishedGutHealthChangedEvent() {
        this.player = null;
        this.playerId = "";
        this.oldValue = 0f;
        this.newValue = 0f;
        this.cause = "";
    }

    public NourishedGutHealthChangedEvent(String playerId, float oldValue, float newValue, String cause, Player player) {
        this.player = player;
        this.playerId = playerId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.cause = cause;
    }
}
