package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;

@ApiStatus.Experimental
public class NourishedSourceConsumedEvent implements KubeEvent {

    public final String playerId;
    public final String itemId;
    public final String nutrientKey;
    public final float amount;

    public NourishedSourceConsumedEvent() {
        this.playerId = "";
        this.itemId = "";
        this.nutrientKey = "";
        this.amount = 0f;
    }

    public NourishedSourceConsumedEvent(MarieEvents.SourceAppliedEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.itemId = event.getSourceId().toString();
        this.nutrientKey = event.getValueKey();
        this.amount = event.getAmount();
    }
}
