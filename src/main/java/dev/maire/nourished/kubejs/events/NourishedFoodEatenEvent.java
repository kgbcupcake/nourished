package dev.maire.nourished.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.Map;

@ApiStatus.Experimental
public class NourishedFoodEatenEvent implements KubeEvent {

    public final Player player;
    public final String itemId;
    public final Map<String, Float> nutrientDeltas;

    public NourishedFoodEatenEvent() {
        this.player = null;
        this.itemId = "";
        this.nutrientDeltas = Map.of();
    }

    public NourishedFoodEatenEvent(Player player, ResourceLocation itemId, Map<String, Float> deltas) {
        this.player = player;
        this.itemId = itemId.toString();
        this.nutrientDeltas = Collections.unmodifiableMap(deltas);
    }
}
