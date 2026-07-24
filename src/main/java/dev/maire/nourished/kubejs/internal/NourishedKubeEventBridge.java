package dev.maire.nourished.kubejs.internal;

import dev.latvian.mods.kubejs.script.ScriptsLoadedEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.api.value.ValueModifierContext;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.maire.nourished.kubejs.NourishedKubeEvents;
import dev.maire.nourished.kubejs.events.NourishedFoodEatenEvent;
import dev.maire.nourished.kubejs.events.NourishedGutHealthChangedEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientChangedEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientCriticalEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientExcessEvent;
import dev.maire.nourished.kubejs.events.NourishedNutrientModifierEvent;
import dev.maire.nourished.kubejs.events.NourishedRawFoodPenaltyEvent;
import dev.maire.nourished.kubejs.events.NourishedSourceConsumedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApiStatus.Internal
public final class NourishedKubeEventBridge {

    private static boolean registered;

    private NourishedKubeEventBridge() {}

    public static void register() {
        if (!NourishedKubeGuard.isPresent() || registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(NourishedKubeEventBridge::onValueChanged);
        NeoForge.EVENT_BUS.addListener(NourishedKubeEventBridge::onValueCritical);
        NeoForge.EVENT_BUS.addListener(NourishedKubeEventBridge::onValueExcess);
        NeoForge.EVENT_BUS.addListener(NourishedKubeEventBridge::onSourceConsumed);
        NeoForge.EVENT_BUS.addListener(NourishedKubeEventBridge::onScriptReload);
    }

    public static float fireNutrientModifier(ValueModifierContext ctx, float amount) {
        if (!isNutrientKey(ctx.valueKey())) {
            return amount;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.NUTRIENT_MODIFIER_ID)) {
            return amount;
        }
        NourishedNutrientModifierEvent kube = new NourishedNutrientModifierEvent(
                ctx.player(),
                ctx.sourceId().toString(),
                ctx.valueKey(),
                amount);
        NourishedKubeEvents.NUTRIENT_MODIFIER.post(kube);
        return kube.amount;
    }

    public static void fireGutHealthChanged(
            String playerId,
            float oldValue,
            float newValue,
            String cause
    ) {
        if (oldValue == newValue) {
            return;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.GUT_HEALTH_CHANGED_ID)) {
            return;
        }
        NourishedKubeEvents.GUT_HEALTH_CHANGED.post(
                new NourishedGutHealthChangedEvent(playerId, oldValue, newValue, cause, resolvePlayer(playerId)));
    }

    public static void fireFoodEaten(ServerPlayer player, String itemId, Map<String, Float> deltas) {
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.FOOD_EATEN_ID)) {
            return;
        }
        ResourceLocation parsedItemId = ResourceLocation.tryParse(itemId);
        if (parsedItemId == null) {
            Nourished.LOGGER.warn("[NourishedKubeEventBridge] Invalid item id '{}' — skipping FOOD_EATEN event", itemId);
            return;
        }
        NourishedKubeEvents.FOOD_EATEN.post(
                new NourishedFoodEatenEvent(player, parsedItemId, deltas));
    }

    public static boolean fireRawFoodPenalty(String playerId, String itemId, String tier) {
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.RAW_FOOD_PENALTY_ID)) {
            return false;
        }
        NourishedRawFoodPenaltyEvent event =
                new NourishedRawFoodPenaltyEvent(playerId, itemId, tier, resolvePlayer(playerId));
        NourishedKubeEvents.RAW_FOOD_PENALTY.post(event);
        return event.isCancelled();
    }

    private static void onValueChanged(MarieEvents.ValueChangedEvent event) {
        if (!isNutrientKey(event.getValueKey())) {
            return;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.NUTRIENT_CHANGED_ID)) {
            return;
        }
        NourishedKubeEvents.NUTRIENT_CHANGED.post(new NourishedNutrientChangedEvent(event));
    }

    private static void onValueCritical(MarieEvents.ValueCriticalEvent event) {
        if (!isNutrientKey(event.getValueKey())) {
            return;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.NUTRIENT_CRITICAL_ID)) {
            return;
        }
        NourishedKubeEvents.NUTRIENT_CRITICAL.post(new NourishedNutrientCriticalEvent(event));
    }

    private static void onValueExcess(MarieEvents.ValueExcessEvent event) {
        if (!isNutrientKey(event.getValueKey())) {
            return;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.NUTRIENT_EXCESS_ID)) {
            return;
        }
        NourishedKubeEvents.NUTRIENT_EXCESS.post(new NourishedNutrientExcessEvent(event));
    }

    private static void onSourceConsumed(MarieEvents.SourceAppliedEvent event) {
        if (!isNutrientKey(event.getValueKey())) {
            return;
        }
        if (!NourishedKubeGuard.hasListeners(NourishedKubeEvents.SOURCE_CONSUMED_ID)) {
            return;
        }
        NourishedKubeEvents.SOURCE_CONSUMED.post(new NourishedSourceConsumedEvent(event));
    }

    private static void onScriptReload(ScriptsLoadedEvent event) {
        NourishedKubeGuard.invalidateCache();
    }

    private static Player resolvePlayer(String playerId) {
        if (playerId == null || playerId.isEmpty()) {
            return null;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        try {
            return server.getPlayerList().getPlayer(UUID.fromString(playerId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isNutrientKey(String key) {
        return nutrientKeys().contains(key);
    }

    private static Set<String> nutrientKeys() {
        return NutrientRegistry.getAll().stream()
                .map(NutrientRegistry.NutrientDef::key)
                .collect(Collectors.toSet());
    }
}
