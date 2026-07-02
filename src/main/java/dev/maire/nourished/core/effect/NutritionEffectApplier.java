package dev.maire.nourished.core.effect;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.config.FeatureFlagCache;
import dev.marie.framework.handler.ReloadGuardListener;
import dev.marie.framework.tracking.TrackingData;
import dev.maire.nourished.core.Nourished;
import dev.maire.nourished.core.nutrition.NutrientRegistry;
import dev.marie.framework.util.MarieEffectUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApiStatus.Internal
public final class NutritionEffectApplier {
    private static final Set<String> warnedPlayers = new HashSet<>();
    private static final Map<UUID, Set<ResourceLocation>> NOURISHED_APPLIED_EFFECTS = new HashMap<>();

    private NutritionEffectApplier() {}

    public static void apply(ServerPlayer player, TrackingData data) {
        if (!FeatureFlagCache.enableEffects()) {
            return;
        }
        if (ReloadGuardListener.isReloadInProgress()) {
            return;
        }
        Map<ResourceLocation, String> seenActiveEffects = new HashMap<>();
        Map<ResourceLocation, EffectRegistry.EffectDef> lastSeenActiveByEffect = new HashMap<>();
        Map<ResourceLocation, EffectRegistry.EffectDef> firstSeenActiveByEffect = new HashMap<>();
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            Holder<MobEffect> effect = MarieEffectUtils.resolveEffect(def.effect()).orElse(null);
            if (effect == null) continue;

            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());

            if (!def.enabled()) {
                applyEffect(player, effectId, effect, false, def);
                continue;
            }

            boolean shouldApply = switch (def.trigger()) {
                case "below" -> data.values.getOrDefault(def.nutrient(), 0f) < def.threshold();
                case "above" -> data.values.getOrDefault(def.nutrient(), 0f) > def.threshold();
                case "all_above" -> {
                    if (def.nutrient().equals("all")) {
                        yield NutrientRegistry.getKeys().stream()
                                .allMatch(k -> data.values.getOrDefault(k, 0f) > def.threshold());
                    } else {
                        yield data.values.getOrDefault(def.nutrient(), 0f) > def.threshold();
                    }
                }
                case "any_below" -> NutrientRegistry.getKeys().stream()
                        .anyMatch(k -> data.values.getOrDefault(k, 0f) < def.threshold());
                case "between" -> {
                    float v = data.values.getOrDefault(def.nutrient(), 0f);
                    yield v >= def.threshold() && v <= def.thresholdMax();
                }
                default -> false;
            };
            EffectConflictDetector.checkAndWarn(def, shouldApply, seenActiveEffects);
            applyEffect(player, effectId, effect, shouldApply, def);
            if (shouldApply) {
                firstSeenActiveByEffect.putIfAbsent(effectId, def);
                lastSeenActiveByEffect.put(effectId, def);
            }
        }

        for (Map.Entry<ResourceLocation, EffectRegistry.EffectDef> entry : lastSeenActiveByEffect.entrySet()) {
            ResourceLocation effectId = entry.getKey();
            EffectRegistry.EffectDef first = firstSeenActiveByEffect.get(effectId);
            EffectRegistry.EffectDef second = entry.getValue();
            if (first == null || second == null || first.id().equals(second.id())) {
                continue;
            }
            String warnKey = player.getStringUUID() + ":" + effectId;
            if (warnedPlayers.add(warnKey)) {
                Nourished.LOGGER.warn(
                        "[NutritionEffectApplier] Effect '{}' is targeted by multiple active rules " +
                                "({} and {}). Last rule wins. Consider consolidating in effects.json.",
                        effectId, first.id(), second.id());
            }
        }
    }

    /** Removes every mob effect id referenced by {@link EffectRegistry} (module off or reload cleanup). */
    public static void clearAll(ServerPlayer player) {
        Set<ResourceLocation> appliedByNourished = NOURISHED_APPLIED_EFFECTS.remove(player.getUUID());
        if (appliedByNourished == null || appliedByNourished.isEmpty()) {
            return;
        }
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            MarieEffectUtils.resolveEffect(def.effect()).ifPresent(holder -> {
                ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
                if (appliedByNourished.contains(effectId)) {
                    player.removeEffect(holder);
                }
            });
        }
    }

    static void clearWarnedPlayers() {
        warnedPlayers.clear();
    }

    private static void applyEffect(ServerPlayer player, ResourceLocation effectId, Holder<MobEffect> effect, boolean enabled, EffectRegistry.EffectDef def) {
        Set<ResourceLocation> ownedEffects = NOURISHED_APPLIED_EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        if (enabled) {
            int durationTicks = def.durationTicks();
            MobEffectInstance existing = player.getEffect(effect);
            if (existing == null || existing.getDuration() < 60) {
                player.addEffect(new MobEffectInstance(
                        effect,
                        durationTicks,
                        def.amplifier(),
                        def.ambient(),
                        def.showParticles(),
                        true));
                ownedEffects.add(effectId);
            }
        } else {
            if (ownedEffects.contains(effectId)) {
                player.removeEffect(effect);
                ownedEffects.remove(effectId);
            }
            if (ownedEffects.isEmpty()) {
                NOURISHED_APPLIED_EFFECTS.remove(player.getUUID());
            }
        }
    }
}
