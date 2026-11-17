package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.world.entity.player.Player;

/**
 * A food group's deficiency tier is about to change from natural build-up or recovery. Set
 * {@code newTier} to steer it, or {@link #cancel()} to keep {@code oldTier}.
 */
@ApiStatus.Experimental
public class NourishedDeficiencyTierChangedEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String group;
    public final int oldTier;
    public int newTier;

    public NourishedDeficiencyTierChangedEvent() {
        this.player = null;
        this.playerId = "";
        this.group = "";
        this.oldTier = 0;
        this.newTier = 0;
    }

    public NourishedDeficiencyTierChangedEvent(Player player, String group, int oldTier, int newTier) {
        this.player = player;
        this.playerId = player.getUUID().toString();
        this.group = group;
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    public boolean isWorsening() {
        return newTier > oldTier;
    }

    public void cancel() {
        this.newTier = oldTier;
    }
}
