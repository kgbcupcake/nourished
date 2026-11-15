package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.world.entity.player.Player;

/**
 * Fired each deficiency check for a group whose pressure is about to move. {@code delta} is in
 * ticks (positive builds deficiency, negative recovers); change it to scale or block the step.
 */
@ApiStatus.Experimental
public class NourishedDeficiencyPressureEvent implements KubeEvent {

    public final Player player;
    public final String playerId;
    public final String group;
    public final float value;
    public final int pressure;
    public int delta;

    public NourishedDeficiencyPressureEvent() {
        this.player = null;
        this.playerId = "";
        this.group = "";
        this.value = 0f;
        this.pressure = 0;
        this.delta = 0;
    }

    public NourishedDeficiencyPressureEvent(Player player, String group, float value, int pressure, int delta) {
        this.player = player;
        this.playerId = player.getUUID().toString();
        this.group = group;
        this.value = value;
        this.pressure = pressure;
        this.delta = delta;
    }

    public void cancel() {
        this.delta = 0;
    }
}
