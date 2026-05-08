package dev.maire.nourished.effect;

import dev.maire.nourished.api.ApiStatus;
import dev.maire.nourished.config.ModuleCache;
import dev.maire.nourished.diet.DietData;
import dev.maire.nourished.nutrition.Nourished;
import dev.maire.nourished.nutrition.NutrientRegistry;
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

@ApiStatus.Internal
public final class NutritionEffectApplier {
    private static final Set<String> warnedPlayers = new HashSet<>();

    private NutritionEffectApplier() {}

    public static void apply(ServerPlayer player, DietData data) {
        if (!ModuleCache.enableEffects) {
            return;
        }
        Map<ResourceLocation, String> seenActiveEffects = new HashMap<>();
        Map<ResourceLocation, EffectRegistry.EffectDef> lastSeenActiveByEffect = new HashMap<>();
        Map<ResourceLocation, EffectRegistry.EffectDef> firstSeenActiveByEffect = new HashMap<>();
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            ResourceLocation effectId = ResourceLocation.parse(def.effect());
            Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT
                    .getHolder(effectId)
                    .orElse(null);
            if (effect == null) continue;

            if (!def.enabled()) {
                player.removeEffect(effect);
                continue;
            }

            boolean shouldApply = switch (def.trigger()) {
                case "below" -> data.nutrients.getOrDefault(def.nutrient(), 0f) < def.threshold();
                case "above" -> data.nutrients.getOrDefault(def.nutrient(), 0f) > def.threshold();
                case "all_above" -> {
                    if (def.nutrient().equals("all")) {
                        yield NutrientRegistry.getKeys().stream()
                                .allMatch(k -> data.nutrients.getOrDefault(k, 0f) > def.threshold());
                    } else {
                        yield data.nutrients.getOrDefault(def.nutrient(), 0f) > def.threshold();
                    }
                }
                case "any_below" -> NutrientRegistry.getKeys().stream()
                        .anyMatch(k -> data.nutrients.getOrDefault(k, 0f) < def.threshold());
                case "between" -> {
                    float v = data.nutrients.getOrDefault(def.nutrient(), 0f);
                    yield v >= def.threshold() && v <= def.thresholdMax();
                }
                default -> false;
            };
            EffectConflictDetector.checkAndWarn(def, shouldApply, seenActiveEffects);
            applyEffect(player, effect, shouldApply, def);
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
        for (EffectRegistry.EffectDef def : EffectRegistry.getAll()) {
            BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceLocation.parse(def.effect()))
                    .ifPresent(holder -> player.removeEffect(holder));
        }
    }

    static void clearWarnedPlayers() {
        warnedPlayers.clear();
    }

    private static void applyEffect(ServerPlayer player, Holder<MobEffect> effect, boolean enabled, EffectRegistry.EffectDef def) {
        if (enabled) {
            int durationTicks = def.durationTicks();
            MobEffectInstance existing = player.getEffect(effect);
            if (existing == null || existing.getDuration() < durationTicks / 2) {
                player.addEffect(new MobEffectInstance(
                        effect,
                        durationTicks,
                        def.amplifier(),
                        def.ambient(),
                        def.showParticles(),
                        true));
            }
        } else {
            player.removeEffect(effect);
        }
    }
}
